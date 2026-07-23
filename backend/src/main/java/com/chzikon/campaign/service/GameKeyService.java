package com.chzikon.campaign.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.GameKey;
import com.chzikon.campaign.dto.GameKeyAdminView;
import com.chzikon.campaign.dto.KeyRegisterResult;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.campaign.repository.GameKeyRepository;
import com.chzikon.global.crypto.KeyCipher;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameKeyService {

    private final GameKeyRepository gameKeyRepository;
    private final CampaignRepository campaignRepository;
    private final KeyCipher keyCipher;
    private final AdminLogService adminLogService;

    /**
     * 키 일괄 등록(붙여넣기): 공백트림 + 빈줄제거 + 중복제거(현재 입력 내 + 기존 캠페인 내) + 암호화 저장.
     * 평문은 메서드 로컬에서만 다루고 즉시 암호화/폐기. 결과 리포트만 반환(평문 비포함).
     */
    @Transactional
    public KeyRegisterResult bulkRegister(Long campaignId, String rawKeys, Long actorId) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String[] lines = rawKeys.split("\\r?\\n");
        int blank = 0;
        int duplicated = 0;
        int registered = 0;

        Set<String> existingFingerprints = new HashSet<>(gameKeyRepository.findFingerprints(campaignId));
        Set<String> batchFingerprints = new HashSet<>();
        List<GameKey> toSave = new ArrayList<>();

        for (String line : lines) {
            String key = line == null ? "" : line.trim();
            if (key.isEmpty()) {
                blank++;
                continue;
            }
            String fp = keyCipher.fingerprint(key);
            if (existingFingerprints.contains(fp) || !batchFingerprints.add(fp)) {
                duplicated++;
                continue;
            }
            toSave.add(new GameKey(campaignId, keyCipher.encrypt(key), fp));
            registered++;
        }
        gameKeyRepository.saveAll(toSave);
        // 키가 등록됐는데 QUANTITY(키 없음) 모드면 자동 전환 — 승인 시 키 자동 배정이 돌게 (항목 15 픽스)
        if (registered > 0) {
            campaign.enableUniqueKeyMode();
        }

        long available = gameKeyRepository.countByCampaignIdAndStatus(campaignId, GameKey.Status.AVAILABLE);
        adminLogService.record(actorId, "KEY_BULK_REGISTER", "campaign", campaignId,
                "registered=" + registered + " duplicated=" + duplicated + " blank=" + blank);
        return new KeyRegisterResult(registered, duplicated, blank, (int) available);
    }

    /** 어드민 키 목록 — 전부 마스킹. 복호화는 마스킹 표시 목적상 4자리만 필요하나 안전하게 평문→마스킹 후 폐기. */
    @Transactional(readOnly = true)
    public List<GameKeyAdminView> listMasked(Long campaignId) {
        return gameKeyRepository.findByCampaignIdOrderByIdAsc(campaignId).stream()
                .map(k -> GameKeyAdminView.of(k, keyCipher.decrypt(k.getKeyValueEnc())))
                .toList();
    }

    /** 배정 전 키 삭제. 배정된 키는 삭제 불가(무효화로). */
    @Transactional
    public void deleteBeforeAssign(Long campaignId, Long keyId, Long actorId) {
        GameKey key = gameKeyRepository.findByIdAndCampaignId(keyId, campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!key.isAvailable()) {
            throw new BusinessException(ErrorCode.KEY_ALREADY_ASSIGNED, "배정된 키는 삭제 대신 무효화하세요.");
        }
        gameKeyRepository.delete(key);
        adminLogService.record(actorId, "KEY_DELETE", "game_key", keyId, "campaign=" + campaignId);
    }

    /** 배정된 키 무효화(재배정 동선). 신청의 키 연결 해제는 별도 ApplicationService 에서. */
    @Transactional
    public void revoke(Long campaignId, Long keyId, Long actorId) {
        GameKey key = gameKeyRepository.findByIdAndCampaignId(keyId, campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        key.revoke();
        adminLogService.record(actorId, "KEY_REVOKE", "game_key", keyId, "campaign=" + campaignId);
    }
}
