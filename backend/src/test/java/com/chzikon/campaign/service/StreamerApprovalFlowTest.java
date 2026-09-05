package com.chzikon.campaign.service;

import com.chzikon.campaign.domain.*;
import com.chzikon.campaign.dto.CampaignCreateRequest;
import com.chzikon.campaign.dto.CampaignUpdateRequest;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.notification.service.NotificationService;
import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.domain.TournamentStatus;
import com.chzikon.tournament.dto.TournamentCreateRequest;
import com.chzikon.tournament.dto.TournamentUpdateRequest;
import com.chzikon.tournament.service.TournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 스트리머 등록분 승인제: 등록=준비중 강제 → 본인이 모집중으로 못 바꿈 → 관리자 승인 시 모집중 + 알림.
 * 스눅 공식(어드민 등록)은 이 규칙 밖.
 */
@SpringBootTest
class StreamerApprovalFlowTest {

    @Autowired CampaignService campaignService;
    @Autowired TournamentService tournamentService;
    @Autowired CampaignRepository campaignRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired NotificationService notificationService;

    private Member streamer(String ch) {
        return memberRepository.save(Member.create(Provider.CHZZK, ch, "스트리머-" + ch, null, 500, Role.STREAMER));
    }

    private Member admin(String ch) {
        return memberRepository.save(Member.create(Provider.CHZZK, ch, "관리자-" + ch, null, 0, Role.ADMIN));
    }

    @Test
    void streamer_campaign_is_forced_preparing_then_admin_approval_opens_it() {
        Member s = streamer("apv-c-s1");
        Member a = admin("apv-c-a1");
        CampaignCreateRequest req = new CampaignCreateRequest("스트리머 컨텐츠", "설명", "게임", null,
                null, null, null, CampaignStatus.OPEN, DistributionType.APPROVAL, KeyMode.QUANTITY,
                5, false, 0, null);
        Campaign saved = campaignService.createByStreamer(req, s.getId());
        assertThat(saved.getStatus()).isEqualTo(CampaignStatus.PREPARING); // OPEN 으로 보내도 준비중
        assertThat(saved.getOwnerMemberId()).isEqualTo(s.getId());

        // 본인이 모집중으로 전환 시도 → 차단
        CampaignUpdateRequest toOpen = new CampaignUpdateRequest(null, null, null, null, null, null, null,
                CampaignStatus.OPEN, null, null, null, null, null, null);
        assertThatThrownBy(() -> campaignService.updateOwned(saved.getId(), toOpen, s.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NEEDS_APPROVAL);
        assertThat(campaignRepository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(CampaignStatus.PREPARING);

        // 본인이 준비중/종료 등 다른 상태로 바꾸는 건 허용
        CampaignUpdateRequest toClosed = new CampaignUpdateRequest(null, null, null, null, null, null, null,
                CampaignStatus.CLOSED, null, null, null, null, null, null);
        campaignService.updateOwned(saved.getId(), toClosed, s.getId());
        campaignService.updateOwned(saved.getId(), new CampaignUpdateRequest(null, null, null, null, null, null, null,
                CampaignStatus.PREPARING, null, null, null, null, null, null), s.getId());

        // 관리자 승인 → 모집중 + 등록자 알림
        long before = notificationService.unreadCount(s.getId());
        Campaign approved = campaignService.approve(saved.getId(), a.getId());
        assertThat(approved.getStatus()).isEqualTo(CampaignStatus.OPEN);
        assertThat(approved.isOpenForApply()).isTrue();
        assertThat(notificationService.unreadCount(s.getId())).isEqualTo(before + 1);

        // 승인된 상태(OPEN)를 본인이 그대로 저장하는 건 허용(제목만 수정 등)
        campaignService.updateOwned(saved.getId(), new CampaignUpdateRequest("제목 수정", null, null, null, null, null, null,
                CampaignStatus.OPEN, null, null, null, null, null, null), s.getId());
        assertThat(campaignRepository.findById(saved.getId()).orElseThrow().getTitle()).isEqualTo("제목 수정");
    }

    @Test
    void admin_created_campaign_keeps_requested_status() {
        Member a = admin("apv-c-a2");
        CampaignCreateRequest req = new CampaignCreateRequest("스눅 공식", null, null, null,
                null, null, null, CampaignStatus.OPEN, DistributionType.FCFS, KeyMode.QUANTITY,
                3, false, 0, null);
        assertThat(campaignService.create(req, a.getId()).getStatus()).isEqualTo(CampaignStatus.OPEN);
    }

    @Test
    void streamer_tournament_is_forced_preparing_then_admin_approval_opens_it() {
        Member s = streamer("apv-t-s1");
        Member a = admin("apv-t-a1");
        TournamentCreateRequest req = new TournamentCreateRequest("스트리머 대회", null, null, null, null,
                null, null, null, 8, TournamentStatus.OPEN, null, null, false, 0);
        Tournament saved = tournamentService.createByStreamer(req, s.getId());
        assertThat(saved.getStatus()).isEqualTo(TournamentStatus.PREPARING);

        TournamentUpdateRequest toOpen = new TournamentUpdateRequest(null, null, null, null, null,
                null, null, null, null, TournamentStatus.OPEN, null, null, null, null);
        assertThatThrownBy(() -> tournamentService.updateOwned(saved.getId(), toOpen, s.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NEEDS_APPROVAL);

        long before = notificationService.unreadCount(s.getId());
        assertThat(tournamentService.approve(saved.getId(), a.getId()).getStatus()).isEqualTo(TournamentStatus.OPEN);
        assertThat(notificationService.unreadCount(s.getId())).isEqualTo(before + 1);
    }
}
