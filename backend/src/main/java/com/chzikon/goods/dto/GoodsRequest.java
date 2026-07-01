package com.chzikon.goods.dto;

import com.chzikon.goods.domain.Goods;
import com.chzikon.goods.domain.GoodsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** 어드민 굿즈 등록/수정. price·stock 은 음수 불가. */
public record GoodsRequest(
        @NotBlank String name,
        String description,
        String imageUrl,
        @PositiveOrZero int price,
        @PositiveOrZero int stock,
        GoodsStatus status,
        int sortOrder
) {
    public Goods toEntity() {
        return Goods.builder()
                .name(name)
                .description(description)
                .imageUrl(imageUrl)
                .price(price)
                .stock(stock)
                .status(status)
                .sortOrder(sortOrder)
                .build();
    }
}
