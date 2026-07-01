package com.chzikon.campaign.service;

import com.chzikon.campaign.domain.*;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-007 / CMP-08: FCFS 마지막 슬롯 동시 클릭 시 초과배정 금지.
 * 슬롯 1개 캠페인에 2명이 동시 신청 → 정확히 1명만 APPROVED, filled_slots=1.
 */
@SpringBootTest
class FcfsConcurrencyTest {

    @Autowired CampaignRepository campaignRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CampaignApplicationRepository applicationRepository;
    @Autowired CampaignApplicationService applicationService;

    @Test
    void only_one_application_succeeds_for_single_slot() throws Exception {
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .title("동시성 테스트")
                .status(CampaignStatus.OPEN)
                .distributionType(DistributionType.FCFS)
                .keyMode(KeyMode.QUANTITY)
                .totalSlots(1)
                .build());

        Member m1 = memberRepository.save(Member.create("ch-1", "스트리머1", null, 100, Role.STREAMER));
        Member m2 = memberRepository.save(Member.create("ch-2", "스트리머2", null, 100, Role.STREAMER));

        AtomicInteger success = new AtomicInteger();
        AtomicInteger full = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        // 두 멤버 동시 신청
        Future<?> f1 = pool.submit(() -> attempt(campaign.getId(), m1.getId(), ready, go, success, full));
        Future<?> f2 = pool.submit(() -> attempt(campaign.getId(), m2.getId(), ready, go, success, full));

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(full.get()).isEqualTo(1);

        Campaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(reloaded.getFilledSlots()).isEqualTo(1);

        List<CampaignApplication> apps = applicationRepository.findByCampaignIdOrderByAppliedAtAsc(campaign.getId());
        long approved = apps.stream().filter(CampaignApplication::isApproved).count();
        assertThat(approved).isEqualTo(1);
    }

    private Void attempt(Long campaignId, Long memberId, CountDownLatch ready, CountDownLatch go,
                         AtomicInteger success, AtomicInteger full) {
        try {
            ready.countDown();
            go.await();
            applicationService.apply(campaignId, memberId);
            success.incrementAndGet();
        } catch (BusinessException e) {
            full.incrementAndGet();
        } catch (Exception e) {
            // 락 경합 외 예외는 실패로 간주(full 아님) — 카운트 안 함
        }
        return null;
    }
}
