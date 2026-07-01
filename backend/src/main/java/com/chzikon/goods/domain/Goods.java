package com.chzikon.goods.domain;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 굿즈 상품. 가격/재고는 정수(원). 결제금액 검증의 기준값이므로 어드민만 변경. */
@Entity
@Table(name = "goods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoodsStatus status = GoodsStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Goods(String name, String description, String imageUrl, int price, int stock,
                 GoodsStatus status, int sortOrder) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.stock = stock;
        this.status = status != null ? status : GoodsStatus.ACTIVE;
        this.sortOrder = sortOrder;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String description, String imageUrl, Integer price,
                       Integer stock, GoodsStatus status, Integer sortOrder) {
        if (name != null) this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        if (price != null) this.price = price;
        if (stock != null) this.stock = stock;
        if (status != null) this.status = status;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPurchasable() {
        return this.status == GoodsStatus.ACTIVE && this.stock > 0;
    }

    /** 재고 선점(주문 생성 시, 비관적 락 트랜잭션 내에서 호출). 부족 시 예외. */
    public void reserveStock(int quantity) {
        if (this.status != GoodsStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.GOODS_NOT_AVAILABLE);
        }
        if (quantity <= 0 || this.stock < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /** 결제 실패/취소 시 재고 환원(선점분 복구). */
    public void restoreStock(int quantity) {
        if (quantity > 0) {
            this.stock += quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }
}
