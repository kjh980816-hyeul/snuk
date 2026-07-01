package com.chzikon.goods.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.goods.domain.GoodsOrder;
import com.chzikon.goods.dto.OrderCreateRequest;
import com.chzikon.goods.dto.OrderResponse;
import com.chzikon.goods.dto.OrderView;
import com.chzikon.goods.config.PortoneProperties;
import com.chzikon.goods.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 주문/결제 — 로그인 필요(본인). 결제창 호출값은 create 응답으로 내려준다. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 주문 생성(재고 선점) → PortOne 결제창 호출값 반환. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest req,
                                                @AuthenticationPrincipal MemberPrincipal principal) {
        GoodsOrder order = orderService.create(req, principal.memberId());
        PortoneProperties p = orderService.portoneProperties();
        return ResponseEntity.ok(OrderResponse.of(order, p.storeId(), p.channelKey()));
    }

    /** 결제창 완료 후 클라이언트 확인 요청 → 서버가 PortOne 재조회로 검증. */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderView> confirm(@PathVariable Long id,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        GoodsOrder owned = orderService.getOwned(id, principal.memberId());
        GoodsOrder reconciled = orderService.reconcile(owned.getPaymentId());
        return ResponseEntity.ok(OrderView.from(reconciled));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderView>> myOrders(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(orderService.myOrders(principal.memberId()).stream()
                .map(OrderView::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderView> detail(@PathVariable Long id,
                                            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(OrderView.from(orderService.getOwned(id, principal.memberId())));
    }
}
