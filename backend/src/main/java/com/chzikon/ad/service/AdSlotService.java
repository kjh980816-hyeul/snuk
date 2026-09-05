package com.chzikon.ad.service;

import com.chzikon.ad.domain.AdSlot;
import com.chzikon.ad.dto.AdSlotDtos.AdSlotRequest;
import com.chzikon.ad.repository.AdSlotRepository;
import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.util.ExternalUrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdSlotService {

    private final AdSlotRepository adSlotRepository;
    private final AdminLogService adminLogService;
    private final ExternalUrlValidator urlValidator;

    /** 공개 — 활성 + 기간 안인 것만, 순서대로. */
    @Transactional(readOnly = true)
    public List<AdSlot> findLive() {
        LocalDateTime now = LocalDateTime.now();
        return adSlotRepository.findByActiveTrueOrderBySortOrderAscIdDesc().stream()
                .filter(a -> a.isLiveAt(now)).toList();
    }

    @Transactional(readOnly = true)
    public List<AdSlot> findAll() {
        return adSlotRepository.findAllByOrderBySortOrderAscIdDesc();
    }

    @Transactional(readOnly = true)
    public AdSlot getById(Long id) {
        return adSlotRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public AdSlot create(AdSlotRequest req, Long actorId) {
        validate(req);
        AdSlot saved = adSlotRepository.save(new AdSlot(req.title(), req.imageUrl(), blankToNull(req.linkUrl()),
                req.activeOrDefault(), req.sortOrderOrDefault(), req.startAt(), req.endAt(), actorId));
        adminLogService.record(actorId, "AD_SLOT_CREATE", "ad_slot", saved.getId(), "title=" + saved.getTitle());
        return saved;
    }

    @Transactional
    public AdSlot update(Long id, AdSlotRequest req, Long actorId) {
        validate(req);
        AdSlot ad = getById(id);
        ad.update(req.title(), req.imageUrl(), blankToNull(req.linkUrl()), req.activeOrDefault(),
                req.sortOrderOrDefault(), req.startAt(), req.endAt());
        adminLogService.record(actorId, "AD_SLOT_UPDATE", "ad_slot", id, "title=" + ad.getTitle());
        return ad;
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        AdSlot ad = getById(id);
        adSlotRepository.delete(ad);
        adminLogService.record(actorId, "AD_SLOT_DELETE", "ad_slot", id, "title=" + ad.getTitle());
    }

    private void validate(AdSlotRequest req) {
        urlValidator.validate(req.imageUrl());           // 업로드 경로(/uploads/) 또는 https
        String link = blankToNull(req.linkUrl());
        // 링크는 사이트 내부 경로(/campaigns 등) 또는 https:// 외부 주소만
        if (link != null && !(link.startsWith("/") && !link.startsWith("//") && !link.contains(".."))) {
            urlValidator.validate(link);
        }
        if (req.startAt() != null && req.endAt() != null && req.endAt().isBefore(req.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종료 시각이 시작 시각보다 빠릅니다.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
