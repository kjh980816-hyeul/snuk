package com.chzikon.goods.client;

/**
 * PortOne 결제 조회 정규화 결과. (PG 응답에서 검증에 필요한 값만 추림)
 * totalAmount = 실제 결제된 총액(원). 서버 주문금액과 반드시 대조.
 */
public record PortonePayment(String status, String transactionId, long totalAmount) {

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    public boolean isFailedOrCancelled() {
        return "FAILED".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status)
                || "PARTIAL_CANCELLED".equalsIgnoreCase(status);
    }
}
