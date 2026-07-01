package com.chzikon.goods.client;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.goods.config.PortoneProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * PortOne(V2) 결제 조회 클라이언트.
 * 결제 검증은 "프론트 통보"가 아니라 이 서버→PortOne API 재조회를 진실의 원천으로 삼는다.
 *   GET {apiBase}/payments/{paymentId}   Authorization: PortOne {apiSecret}
 * 응답 예: { "status":"PAID", "transactionId":"...", "amount": { "total": 10000, ... } }
 * 문서: https://developers.portone.io/api/rest-v2
 */
@Slf4j
@Component
public class PortoneClient {

    private final PortoneProperties props;
    private final RestClient client;

    public PortoneClient(PortoneProperties props, RestClient.Builder builder) {
        this.props = props;
        this.client = builder.clone().baseUrl(props.apiBaseUri()).build();
    }

    /** paymentId 로 실제 결제 상태/금액을 조회. 조회 자체 실패는 PAYMENT_VERIFY_FAILED. */
    public PortonePayment getPayment(String paymentId) {
        try {
            JsonNode res = client.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .header("Authorization", "PortOne " + props.apiSecret())
                    .retrieve()
                    .body(JsonNode.class);
            if (res == null) {
                throw new BusinessException(ErrorCode.PAYMENT_VERIFY_FAILED);
            }
            String status = text(res, "status");
            String txId = text(res, "transactionId");
            long total = res.path("amount").path("total").asLong(-1);
            if (status == null || total < 0) {
                log.warn("portone payment parse failed: paymentId={}", paymentId);
                throw new BusinessException(ErrorCode.PAYMENT_VERIFY_FAILED);
            }
            return new PortonePayment(status, txId, total);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("portone getPayment failed: paymentId={} err={}", paymentId, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_VERIFY_FAILED);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull() && !v.asText().isBlank()) ? v.asText() : null;
    }
}
