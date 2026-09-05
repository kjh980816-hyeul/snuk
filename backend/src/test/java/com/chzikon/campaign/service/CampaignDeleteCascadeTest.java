package com.chzikon.campaign.service;

import com.chzikon.campaign.domain.*;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.campaign.repository.GameKeyRepository;
import com.chzikon.collab.domain.CollabGame;
import com.chzikon.collab.repository.CollabGameRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 컨텐츠 삭제 캐스케이드(2026-08-07 검증 잔여 버그).
 * - 미배정 키·대기 신청만 있으면 함께 지워지고, 연결 게임은 링크만 해제된다(과거엔 FK 500).
 * - 키가 배정된(승인된) 신청이 있으면 M009 로 막는다(게임 키 = 금전가치).
 */
@SpringBootTest
class CampaignDeleteCascadeTest {

    @Autowired CampaignService campaignService;
    @Autowired CampaignRepository campaignRepository;
    @Autowired GameKeyRepository gameKeyRepository;
    @Autowired CampaignApplicationRepository applicationRepository;
    @Autowired CollabGameRepository collabGameRepository;
    @Autowired MemberRepository memberRepository;

    private Campaign newCampaign(String title) {
        return campaignRepository.save(Campaign.builder()
                .title(title).status(CampaignStatus.OPEN)
                .distributionType(DistributionType.APPROVAL).keyMode(KeyMode.UNIQUE_KEY)
                .totalSlots(3).build());
    }

    @Test
    void delete_cascades_unassigned_keys_pending_applications_and_unlinks_game() {
        Campaign c = newCampaign("삭제 캐스케이드");
        Member admin = memberRepository.save(Member.create(Provider.CHZZK, "del-adm-1", "관리자", null, 0, Role.ADMIN));
        Member streamer = memberRepository.save(Member.create(Provider.CHZZK, "del-st-1", "스트리머", null, 100, Role.STREAMER));
        gameKeyRepository.save(new GameKey(c.getId(), "enc-1", "fp-1"));
        gameKeyRepository.save(new GameKey(c.getId(), "enc-2", "fp-2"));
        applicationRepository.save(new CampaignApplication(c.getId(), streamer.getId(), 100, CampaignApplication.Status.PENDING));
        CollabGame game = collabGameRepository.save(new CollabGame("연결 게임", null, null, null, null, c.getId(), 0));

        campaignService.delete(c.getId(), admin.getId());

        assertThat(campaignRepository.findById(c.getId())).isEmpty();
        assertThat(gameKeyRepository.findByCampaignIdOrderByIdAsc(c.getId())).isEmpty();
        assertThat(applicationRepository.findByCampaignIdOrderByAppliedAtAsc(c.getId())).isEmpty();
        CollabGame reloaded = collabGameRepository.findById(game.getId()).orElseThrow();
        assertThat(reloaded.getCampaignId()).isNull(); // 게임은 남고 링크만 해제
    }

    @Test
    void delete_is_blocked_when_a_key_is_already_assigned() {
        Campaign c = newCampaign("배정 있음");
        Member admin = memberRepository.save(Member.create(Provider.CHZZK, "del-adm-2", "관리자", null, 0, Role.ADMIN));
        Member streamer = memberRepository.save(Member.create(Provider.CHZZK, "del-st-2", "스트리머", null, 100, Role.STREAMER));
        GameKey key = gameKeyRepository.save(new GameKey(c.getId(), "enc-3", "fp-3"));
        key.assignTo(streamer.getId());
        gameKeyRepository.save(key);
        CampaignApplication app = new CampaignApplication(c.getId(), streamer.getId(), 100, CampaignApplication.Status.PENDING);
        app.approve(key.getId());
        applicationRepository.save(app);

        assertThatThrownBy(() -> campaignService.delete(c.getId(), admin.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CAMPAIGN_HAS_ASSIGNMENTS);

        assertThat(campaignRepository.findById(c.getId())).isPresent();
        assertThat(gameKeyRepository.findByCampaignIdOrderByIdAsc(c.getId())).hasSize(1);
    }

    @Test
    void get_missing_campaign_raises_not_found() {
        assertThatThrownBy(() -> campaignService.getById(9_999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
