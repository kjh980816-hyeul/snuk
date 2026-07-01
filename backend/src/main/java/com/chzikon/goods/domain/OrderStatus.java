package com.chzikon.goods.domain;

/**
 * 주문/결제 상태.
 * PENDING  = 주문 생성(재고 선점) · 결제 대기
 * PAID     = PG 결제 검증 완료(금액 일치)
 * CANCELLED= 결제 취소/환불 (다음 단계)
 * FAILED   = 결제 실패/금액 불일치 (재고 환원)
 */
public enum OrderStatus {
    PENDING, PAID, CANCELLED, FAILED
}
