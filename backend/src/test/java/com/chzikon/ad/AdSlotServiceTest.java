package com.chzikon.ad;

import com.chzikon.ad.domain.AdSlot;
import com.chzikon.ad.dto.AdSlotDtos.AdSlotRequest;
import com.chzikon.ad.service.AdSlotService;
import com.chzikon.global.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 광고 슬롯: 공개 목록은 활성+기간 안만, 순서 정렬. 잘못된 링크/기간은 거절. */
@SpringBootTest
class AdSlotServiceTest {

    @Autowired AdSlotService service;

    @Test
    void public_list_returns_only_live_slots_in_order() {
        LocalDateTime now = LocalDateTime.now();
        AdSlot a2 = service.create(new AdSlotRequest("두번째", "/uploads/ad2.png", "https://example.com/2", true, 2, null, null), 1L);
        AdSlot a1 = service.create(new AdSlotRequest("첫번째", "/uploads/ad1.png", null, true, 1, null, null), 1L);
        service.create(new AdSlotRequest("꺼짐", "/uploads/off.png", null, false, 0, null, null), 1L);
        service.create(new AdSlotRequest("만료", "/uploads/old.png", null, true, 0, now.minusDays(10), now.minusDays(1)), 1L);
        service.create(new AdSlotRequest("예약", "/uploads/future.png", null, true, 0, now.plusDays(1), null), 1L);
        AdSlot inWindow = service.create(new AdSlotRequest("기간내", "/uploads/win.png", null, true, 0, now.minusHours(1), now.plusHours(1)), 1L);

        List<Long> ids = service.findLive().stream().map(AdSlot::getId).toList();
        assertThat(ids).containsExactly(inWindow.getId(), a1.getId(), a2.getId());
        assertThat(service.findAll()).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void rejects_bad_link_and_inverted_period() {
        assertThatThrownBy(() -> service.create(new AdSlotRequest("x", "/uploads/a.png", "javascript:alert(1)", true, 0, null, null), 1L))
                .isInstanceOf(BusinessException.class);
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> service.create(new AdSlotRequest("x", "/uploads/a.png", null, true, 0, now, now.minusDays(1)), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void update_and_delete_round_trip() {
        AdSlot a = service.create(new AdSlotRequest("t", "/uploads/a.png", null, true, 0, null, null), 1L);
        service.update(a.getId(), new AdSlotRequest("t2", "/uploads/b.png", "https://snuk.kr/campaigns", false, 5, null, null), 1L);
        AdSlot got = service.getById(a.getId());
        assertThat(got.getTitle()).isEqualTo("t2");
        assertThat(got.isActive()).isFalse();
        assertThat(got.getLinkUrl()).isEqualTo("https://snuk.kr/campaigns");
        service.delete(a.getId(), 1L);
        assertThatThrownBy(() -> service.getById(a.getId())).isInstanceOf(BusinessException.class);
    }
}
