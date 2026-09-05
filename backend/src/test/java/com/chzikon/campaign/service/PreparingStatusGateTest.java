package com.chzikon.campaign.service;

import com.chzikon.campaign.domain.*;
import com.chzikon.campaign.dto.CampaignResponse;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.domain.TournamentStatus;
import com.chzikon.tournament.dto.TournamentResponse;
import com.chzikon.tournament.repository.TournamentRepository;
import com.chzikon.tournament.service.TournamentParticipantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 준비중(PREPARING) 상태 — 내용은 공개되지만 신청은 서버가 전용 코드로 차단해야 한다.
 * 프론트 버튼 숨김과 무관하게 API 직접 호출도 막힌다(백엔드 강제 원칙).
 */
@SpringBootTest
class PreparingStatusGateTest {

    @Autowired CampaignRepository campaignRepository;
    @Autowired TournamentRepository tournamentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CampaignApplicationService applicationService;
    @Autowired TournamentParticipantService participantService;

    @Test
    void preparing_campaign_rejects_apply_with_dedicated_code_and_exposes_flags() {
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .title("준비중 컨텐츠")
                .status(CampaignStatus.PREPARING)
                .distributionType(DistributionType.FCFS)
                .keyMode(KeyMode.QUANTITY)
                .totalSlots(5)
                .build());
        Member m = memberRepository.save(Member.create(Provider.CHZZK, "prep-c-1", "스트리머P", null, 100, Role.STREAMER));

        assertThatThrownBy(() -> applicationService.apply(campaign.getId(), m.getId(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CAMPAIGN_PREPARING);

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(reloaded.getFilledSlots()).isZero();

        CampaignResponse res = CampaignResponse.from(reloaded);
        assertThat(res.status()).isEqualTo("PREPARING");
        assertThat(res.preparing()).isTrue();
        assertThat(res.applyOpen()).isFalse();
    }

    @Test
    void open_campaign_exposes_apply_open_true() {
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .title("모집중 컨텐츠")
                .status(CampaignStatus.OPEN)
                .distributionType(DistributionType.FCFS)
                .keyMode(KeyMode.QUANTITY)
                .totalSlots(5)
                .build());
        CampaignResponse res = CampaignResponse.from(campaign);
        assertThat(res.applyOpen()).isTrue();
        assertThat(res.preparing()).isFalse();
    }

    @Test
    void preparing_tournament_rejects_apply_with_dedicated_code_and_exposes_flags() {
        Tournament tour = tournamentRepository.save(Tournament.builder()
                .title("준비중 대회")
                .capacity(8)
                .status(TournamentStatus.PREPARING)
                .build());
        Member m = memberRepository.save(Member.create(Provider.CHZZK, "prep-t-1", "스트리머T", null, 100, Role.STREAMER));

        assertThatThrownBy(() -> participantService.apply(tour.getId(), m.getId(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOURNAMENT_PREPARING);

        TournamentResponse res = TournamentResponse.from(tournamentRepository.findById(tour.getId()).orElseThrow());
        assertThat(res.status()).isEqualTo("PREPARING");
        assertThat(res.preparing()).isTrue();
        assertThat(res.applyOpen()).isFalse();
    }
}
