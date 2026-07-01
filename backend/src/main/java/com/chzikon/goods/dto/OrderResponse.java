package com.chzikon.goods.dto;

import com.chzikon.goods.domain.GoodsOrder;

/**
 * 주문 생성 응답 = 프론트 PortOne 결제창 호출에 필요한 값 묶음.
 * storeId/channelKey 는 공개값. amount 는 서버 확정 금액(프론트는 이 값을 그대로 전달).
 */
public record OrderResponse(
        Long orderId,
        String paymentId,
        String orderName,
        int amount,
        String status,
        String storeId,
        String channelKey
) {
    public static OrderResponse of(GoodsOrder order, String storeId, String channelKey) {
        return new OrderResponse(
                order.getId(),
                order.getPaymentId(),
                order.getGoodsName(),
                order.getTotalAmount(),
                order.getStatus().name(),
                storeId,
                channelKey);
    }
}
