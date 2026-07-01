package com.chzikon.goods.dto;

import com.chzikon.goods.domain.GoodsOrder;

import java.time.LocalDateTime;

/**
 * 주문 상세/목록 뷰(본인 조회 + 어드민 조회 공용).
 * memberId 는 어드민 목록 식별용 — 본인 조회에서도 자기 값이라 노출 무방.
 */
public record OrderView(
        Long id,
        String paymentId,
        Long memberId,
        Long goodsId,
        String goodsName,
        int quantity,
        int unitPrice,
        int totalAmount,
        String status,
        String receiverName,
        String receiverPhone,
        String zipcode,
        String address,
        String addressDetail,
        String memo,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static OrderView from(GoodsOrder o) {
        return new OrderView(
                o.getId(), o.getPaymentId(), o.getMemberId(), o.getGoodsId(), o.getGoodsName(),
                o.getQuantity(), o.getUnitPrice(), o.getTotalAmount(), o.getStatus().name(),
                o.getReceiverName(), o.getReceiverPhone(), o.getZipcode(), o.getAddress(),
                o.getAddressDetail(), o.getMemo(), o.getPaidAt(), o.getCreatedAt());
    }
}
