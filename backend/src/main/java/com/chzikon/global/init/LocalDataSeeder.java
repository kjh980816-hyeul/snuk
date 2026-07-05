package com.chzikon.global.init;

import com.chzikon.campaign.domain.*;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.collab.domain.ClientLogo;
import com.chzikon.collab.domain.CollabGame;
import com.chzikon.collab.domain.ContentVideo;
import com.chzikon.collab.repository.ClientLogoRepository;
import com.chzikon.collab.repository.CollabGameRepository;
import com.chzikon.collab.repository.ContentVideoRepository;
import com.chzikon.goods.domain.Goods;
import com.chzikon.goods.domain.GoodsStatus;
import com.chzikon.goods.repository.GoodsRepository;
import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.domain.TournamentStatus;
import com.chzikon.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 로컬 개발용 더미 데이터 시더 (local 프로파일 전용).
 * ⚠️ 운영(prod)에서는 절대 실행되지 않음 — CLAUDE.md "운영엔 더미 금지, 데이터 없으면 empty state" 준수.
 * 각 도메인은 "비어 있을 때만" 삽입(멱등) → 재기동/테스트에서 중복·간섭 없음.
 * 삽입 값은 어드민에서 그대로 수정/삭제 가능하도록 모든 필드를 채우고, 이미지/영상 URL 은
 * 검증기(https/공개호스트)를 통과하는 실제 URL 사용(수정 저장 시 재검증에도 통과).
 */
@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class LocalDataSeeder implements ApplicationRunner {

    private final CampaignRepository campaignRepository;
    private final ContentVideoRepository videoRepository;
    private final CollabGameRepository collabGameRepository;
    private final ClientLogoRepository clientLogoRepository;
    private final GoodsRepository goodsRepository;
    private final TournamentRepository tournamentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedGoods();
        seedVideos();
        seedCollabGames();
        seedClients();
        seedCampaigns();
        seedTournaments();
    }

    private void seedGoods() {
        if (goodsRepository.count() > 0) return;
        goodsRepository.save(Goods.builder()
                .name("SNUK 로고 티셔츠")
                .description("스트리머 콜라보 기념 오버핏 반팔 티셔츠. 블랙/화이트.")
                .imageUrl("https://placehold.co/600x600/1a1a1a/ffffff?text=SNUK+Tee")
                .price(25000).stock(50).status(GoodsStatus.ACTIVE).sortOrder(0).build());
        goodsRepository.save(Goods.builder()
                .name("SNUK 아크릴 키링")
                .description("귀여운 마스코트 아크릴 키링. 랜덤 1종.")
                .imageUrl("https://placehold.co/600x600/f9a825/1a1a1a?text=Keyring")
                .price(8000).stock(120).status(GoodsStatus.ACTIVE).sortOrder(1).build());
        goodsRepository.save(Goods.builder()
                .name("SNUK 스티커 팩")
                .description("방송용 데코 스티커 12종 세트.")
                .imageUrl("https://placehold.co/600x600/e64a19/ffffff?text=Stickers")
                .price(5000).stock(0).status(GoodsStatus.ACTIVE).sortOrder(2).build()); // 품절 상태 확인용
        goodsRepository.save(Goods.builder()
                .name("[준비중] 시즌2 후드집업")
                .description("공개 예정 상품(숨김 상태 — 어드민에만 노출).")
                .imageUrl("https://placehold.co/600x600/455a64/ffffff?text=Coming+Soon")
                .price(49000).stock(30).status(GoodsStatus.HIDDEN).sortOrder(3).build());
        log.info("[seed] goods 4건 삽입");
    }

    private void seedVideos() {
        if (videoRepository.count() > 0) return;
        videoRepository.save(new ContentVideo("스트리머 합동 게임 하이라이트",
                "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                "https://placehold.co/640x360/1a1a1a/ffffff?text=Highlight", true, 0));
        videoRepository.save(new ContentVideo("신작 게임 첫 방송 리뷰",
                "https://www.youtube.com/watch?v=ScMzIvxBSi4",
                "https://placehold.co/640x360/263238/ffffff?text=Review", false, 1));
        videoRepository.save(new ContentVideo("콜라보 이벤트 비하인드",
                "https://www.youtube.com/watch?v=ysz5S6PUM-U",
                "https://placehold.co/640x360/37474f/ffffff?text=Behind", false, 2));
        log.info("[seed] content_video 3건 삽입");
    }

    private void seedCollabGames() {
        if (collabGameRepository.count() > 0) return;
        collabGameRepository.save(new CollabGame("네온 서바이벌",
                "스트리머 초청 배틀로얄 콜라보. 시청자 참여전 진행.",
                "https://placehold.co/400x300/1a1a1a/f9a825?text=Neon",
                "https://store.example.com/neon", "https://blog.example.com/neon-review", 1L, 0));
        collabGameRepository.save(new CollabGame("픽셀 던전 러시",
                "인디 로그라이크 협찬 콜라보. 키 배포 이벤트 동반.",
                "https://placehold.co/400x300/263238/ffffff?text=Pixel",
                "https://store.example.com/pixel", null, null, 1));
        log.info("[seed] collab_game 2건 삽입");
    }

    private void seedClients() {
        if (clientLogoRepository.count() > 0) return;
        clientLogoRepository.save(new ClientLogo("게임퍼블리셔 A",
                "https://placehold.co/200x80/ffffff/1a1a1a?text=Client+A", "https://clienta.example.com", 0));
        clientLogoRepository.save(new ClientLogo("인디스튜디오 B",
                "https://placehold.co/200x80/ffffff/1a1a1a?text=Client+B", "https://clientb.example.com", 1));
        clientLogoRepository.save(new ClientLogo("이스포츠팀 C",
                "https://placehold.co/200x80/ffffff/1a1a1a?text=Client+C", null, 2));
        log.info("[seed] client_logo 3건 삽입");
    }

    private void seedCampaigns() {
        if (campaignRepository.count() > 0) return;
        LocalDateTime now = LocalDateTime.now();
        campaignRepository.save(Campaign.builder()
                .title("네온 서바이벌 스트리머 체험단")
                .description("신작 배틀로얄 사전 체험 + 방송 콜라보 모집. 선착순.")
                .gameName("네온 서바이벌")
                .promoImageUrl("https://placehold.co/800x600/1a1a1a/f9a825?text=Neon+Campaign")
                .eventDate(LocalDate.now().plusDays(14))
                .applyStart(now.minusDays(2)).applyEnd(now.plusDays(10))
                .status(CampaignStatus.OPEN).distributionType(DistributionType.FCFS)
                .keyMode(KeyMode.QUANTITY).totalSlots(20).featured(true).sortOrder(0).build());
        campaignRepository.save(Campaign.builder()
                .title("픽셀 던전 러시 키 배포")
                .description("스팀 키 배포 이벤트. 승인제로 진행됩니다.")
                .gameName("픽셀 던전 러시")
                .promoImageUrl("https://placehold.co/800x600/263238/ffffff?text=Pixel+Keys")
                .eventDate(LocalDate.now().plusDays(7))
                .applyStart(now.minusDays(1)).applyEnd(now.plusDays(5))
                .status(CampaignStatus.OPEN).distributionType(DistributionType.APPROVAL)
                .keyMode(KeyMode.QUANTITY).totalSlots(10).featured(false).sortOrder(1).build());
        campaignRepository.save(Campaign.builder()
                .title("[마감] 시즌1 콜라보 모집")
                .description("종료된 캠페인 예시(CLOSED 상태 노출 확인용).")
                .gameName("클래식 아레나")
                .promoImageUrl("https://placehold.co/800x600/455a64/ffffff?text=Closed")
                .eventDate(LocalDate.now().minusDays(10))
                .status(CampaignStatus.CLOSED).distributionType(DistributionType.FCFS)
                .keyMode(KeyMode.QUANTITY).totalSlots(15).featured(false).sortOrder(2).build());
        log.info("[seed] campaign 3건 삽입");
    }

    private void seedTournaments() {
        if (tournamentRepository.count() > 0) return;
        LocalDateTime now = LocalDateTime.now();
        tournamentRepository.save(Tournament.builder()
                .title("SNUK 컵 시즌1 — 배틀로얄 스트리머 대회")
                .description("스트리머 16인 초청 배틀로얄 대회. 참가 신청 후 승인제로 확정됩니다.")
                .gameName("네온 서바이벌")
                .bannerImageUrl("https://placehold.co/1200x400/1a1a1a/f9a825?text=SNUK+CUP+S1")
                .eventDate(LocalDate.now().plusDays(21))
                .applyStart(now.minusDays(1)).applyEnd(now.plusDays(14))
                .capacity(16).status(TournamentStatus.OPEN)
                .featured(true).sortOrder(0).build());
        tournamentRepository.save(Tournament.builder()
                .title("픽셀 던전 스피드런 챌린지")
                .description("클리어 타임 경쟁전. 참가 확정자에게 개별 안내.")
                .gameName("픽셀 던전 러시")
                .bannerImageUrl("https://placehold.co/1200x400/263238/ffffff?text=Speedrun")
                .eventDate(LocalDate.now().plusDays(35))
                .capacity(8).status(TournamentStatus.SCHEDULED)
                .featured(false).sortOrder(1).build());
        tournamentRepository.save(Tournament.builder()
                .title("[종료] 클래식 아레나 쇼매치")
                .description("종료된 대회 예시(DONE 상태 + 결과 노출 확인용).")
                .gameName("클래식 아레나")
                .bannerImageUrl("https://placehold.co/1200x400/455a64/ffffff?text=Show+Match")
                .eventDate(LocalDate.now().minusDays(14))
                .capacity(8).status(TournamentStatus.DONE)
                .resultText("우승: 팀 알파 / 준우승: 팀 브라보 / MVP: 스트리머A")
                .featured(false).sortOrder(2).build());
        log.info("[seed] tournament 3건 삽입");
    }
}
