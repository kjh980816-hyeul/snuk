package com.chzikon.goods.controller;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.goods.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortOne 결제 웹훅 수신(공개 엔드포인트, JWT 없음).
 * 바디의 금액은 신뢰하지 않고 paymentId 만 추출 → 서버가 PortOne API 재조회로 검증(OrderService.reconcile).
 * 알 수 없는 paymentId(스팸/오배송)는 200 으로 흘려보내 재시도를 멈춘다.
 * ⚠️ 웹훅 서명 검증(Standard Webhooks / webhook-secret)은 다음 단계 강화 항목.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody(required = false) JsonNode body) {
        String paymentId = extractPaymentId(body);
        if (paymentId == null) {
            return ResponseEntity.ok().build();
        }
        try {
            orderService.reconcile(paymentId);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
                return ResponseEntity.ok().build(); // 우리 주문이 아님 — 재시도 불필요
            }
            throw e; // 검증 일시 실패 등은 에러로 반환해 PortOne 재시도 유도
        }
        return ResponseEntity.ok().build();
    }

    /** { "data": { "paymentId": "..." } } 또는 루트 paymentId 양쪽 대응. */
    private String extractPaymentId(JsonNode body) {
        if (body == null) {
            return null;
        }
        JsonNode fromData = body.path("data").path("paymentId");
        if (!fromData.isMissingNode() && !fromData.isNull() && !fromData.asText().isBlank()) {
            return fromData.asText();
        }
        JsonNode root = body.path("paymentId");
        return (!root.isMissingNode() && !root.isNull() && !root.asText().isBlank()) ? root.asText() : null;
    }
}
