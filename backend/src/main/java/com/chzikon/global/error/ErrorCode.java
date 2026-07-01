package com.chzikon.global.error;

import org.springframework.http.HttpStatus;

/**
 * 도메인 공통 에러코드. @RestControllerAdvice 에서 일관 응답으로 매핑.
 */
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "대상을 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C004", "권한이 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "C005", "요청이 현재 상태와 충돌합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C500", "서버 오류가 발생했습니다."),

    // 인증/권한 (AUTH)
    OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "A001", "치지직 인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    INSUFFICIENT_ROLE(HttpStatus.FORBIDDEN, "A003", "스트리머 이상 권한이 필요합니다."),

    // 캠페인 (CMP)
    CAMPAIGN_NOT_OPEN(HttpStatus.CONFLICT, "M001", "신청 가능한 상태가 아닙니다."),
    CAMPAIGN_FULL(HttpStatus.CONFLICT, "M002", "모집 슬롯이 모두 소진되었습니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "M003", "이미 신청한 캠페인입니다."),
    FOLLOWER_THRESHOLD_NOT_MET(HttpStatus.FORBIDDEN, "M004", "팔로워 임계값을 충족하지 않습니다."),
    NO_AVAILABLE_KEY(HttpStatus.CONFLICT, "M005", "배정 가능한 키가 없습니다."),
    KEY_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "M006", "이미 배정된 키입니다."),

    // 후기 (REV)
    REVIEW_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "R001", "참가자만 후기를 작성할 수 있습니다."),

    // 굿즈/결제 (GOODS)
    GOODS_NOT_AVAILABLE(HttpStatus.CONFLICT, "G001", "판매 중인 상품이 아닙니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "G002", "재고가 부족합니다."),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "G003", "본인의 주문이 아닙니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "G004", "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_NOT_PAID(HttpStatus.CONFLICT, "G005", "아직 결제가 완료되지 않았습니다."),
    PAYMENT_VERIFY_FAILED(HttpStatus.BAD_GATEWAY, "G006", "결제 검증에 실패했습니다."),

    // 설정 (ADM)
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "설정값을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
