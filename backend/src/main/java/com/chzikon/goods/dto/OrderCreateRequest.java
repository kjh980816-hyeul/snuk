package com.chzikon.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 주문 생성 요청. 금액은 서버가 상품가×수량으로 재계산하므로 클라이언트에서 받지 않는다(위변조 차단).
 */
public record OrderCreateRequest(
        @Positive Long goodsId,
        @Positive int quantity,
        @NotBlank String receiverName,
        @NotBlank String receiverPhone,
        String zipcode,
        @NotBlank String address,
        String addressDetail,
        String memo
) {
}
