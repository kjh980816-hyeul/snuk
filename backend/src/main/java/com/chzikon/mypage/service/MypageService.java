package com.chzikon.mypage.service;

import com.chzikon.campaign.domain.Campaign;
import com.chzikon.campaign.domain.CampaignApplication;
import com.chzikon.campaign.domain.GameKey;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.campaign.repository.GameKeyRepository;
import com.chzikon.global.crypto.KeyCipher;
import com.chzikon.goods.repository.GoodsOrderRepository;
import com.chzikon.mypage.dto.MypageSummaryResponse;
import com.chzikon.mypage.dto.MypageSummaryResponse.*;
import com.chzikon.review.repository.PostRepository;
import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.repository.TournamentParticipantRepository;
import com.chzikon.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 마이페이지 집계(MY-01) — 본인(memberId) 데이터만. 키는 항상 마스킹. */
@Service
@RequiredArgsConstructor
public class MypageService {

    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;
    private final GameKeyRepository gameKeyRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentRepository tournamentRepository;
    private final PostRepository postRepository;
    private final GoodsOrderRepository orderRepository;
    private final KeyCipher keyCipher;

    @Transactional(readOnly = true)
    public MypageSummaryResponse summary(Long memberId) {
        return new MypageSummaryResponse(
                myApplications(memberId),
                myTournaments(memberId),
                myReviews(memberId),
                myOrders(memberId));
    }

    private List<MyCampaignItem> myApplications(Long memberId) {
        List<CampaignApplication> apps = applicationRepository.findByMemberIdOrderByAppliedAtDesc(memberId);
        Map<Long, Campaign> campaigns = campaignRepository.findAllById(
                        apps.stream().map(CampaignApplication::getCampaignId).distinct().toList()).stream()
                .collect(Collectors.toMap(Campaign::getId, Function.identity()));
        return apps.stream().map(a -> {
            Campaign c = campaigns.get(a.getCampaignId());
            String maskedKey = null;
            if (a.getAssignedKeyId() != null) {
                GameKey key = gameKeyRepository.findById(a.getAssignedKeyId()).orElse(null);
                if (key != null) {
                    maskedKey = KeyCipher.mask(keyCipher.decrypt(key.getKeyValueEnc()));
                }
            }
            return new MyCampaignItem(a.getId(), a.getCampaignId(),
                    c != null ? c.getTitle() : "(삭제된 캠페인)",
                    a.getStatus().name(), maskedKey != null, maskedKey, a.getAppliedAt());
        }).toList();
    }

    private List<MyTournamentItem> myTournaments(Long memberId) {
        var participations = participantRepository.findByMemberIdOrderByAppliedAtDesc(memberId);
        Map<Long, Tournament> tournaments = tournamentRepository.findAllById(
                        participations.stream().map(p -> p.getTournamentId()).distinct().toList()).stream()
                .collect(Collectors.toMap(Tournament::getId, Function.identity()));
        return participations.stream().map(p -> {
            Tournament t = tournaments.get(p.getTournamentId());
            return new MyTournamentItem(p.getId(), p.getTournamentId(),
                    t != null ? t.getTitle() : "(삭제된 대회)",
                    p.getStatus().name(), p.getAppliedAt());
        }).toList();
    }

    private List<MyReviewItem> myReviews(Long memberId) {
        return postRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(post -> new MyReviewItem(post.getId(), post.getCampaignId(),
                        post.getTitle(), post.isHidden(), post.getCreatedAt()))
                .toList();
    }

    private List<MyOrderItem> myOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(o -> new MyOrderItem(o.getId(), o.getGoodsName(), o.getQuantity(),
                        o.getTotalAmount(), o.getStatus().name(), o.getCreatedAt()))
                .toList();
    }
}
