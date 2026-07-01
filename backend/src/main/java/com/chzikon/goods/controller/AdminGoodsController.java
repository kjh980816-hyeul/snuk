package com.chzikon.goods.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.goods.dto.GoodsRequest;
import com.chzikon.goods.dto.GoodsResponse;
import com.chzikon.goods.dto.OrderView;
import com.chzikon.goods.service.GoodsService;
import com.chzikon.goods.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 어드민 굿즈/주문 운영. ADMIN 강제(2중화: SecurityConfig + @PreAuthorize). */
@RestController
@RequestMapping("/api/admin/goods")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminGoodsController {

    private final GoodsService goodsService;
    private final OrderService orderService;

    // ----- 상품 CRUD -----
    @GetMapping
    public ResponseEntity<List<GoodsResponse>> list() {
        return ResponseEntity.ok(goodsService.listAll().stream().map(GoodsResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<GoodsResponse> create(@Valid @RequestBody GoodsRequest req,
                                                @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(GoodsResponse.from(goodsService.create(req, principal.memberId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoodsResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody GoodsRequest req,
                                                @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(GoodsResponse.from(goodsService.update(id, req, principal.memberId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        goodsService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ----- 주문 목록(배송/정산 확인) -----
    @GetMapping("/orders")
    public ResponseEntity<List<OrderView>> orders() {
        return ResponseEntity.ok(orderService.listAll().stream().map(OrderView::from).toList());
    }
}
