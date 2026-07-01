package com.chzikon.goods.dto;

import com.chzikon.goods.domain.Goods;

/** 굿즈 상품 공개/어드민 공통 응답. 민감정보 없음. */
public record GoodsResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        int price,
        int stock,
        String status,
        int sortOrder,
        boolean purchasable
) {
    public static GoodsResponse from(Goods g) {
        return new GoodsResponse(
                g.getId(), g.getName(), g.getDescription(), g.getImageUrl(),
                g.getPrice(), g.getStock(), g.getStatus().name(), g.getSortOrder(),
                g.isPurchasable());
    }
}
