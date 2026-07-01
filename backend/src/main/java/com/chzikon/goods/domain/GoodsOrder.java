package com.chzikon.goods.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 굿즈 주문. 결제금액(totalAmount)은 주문 시점 서버 계산값 스냅샷 —
 * PG 결제 응답의 금액과 대조해 위변조를 잡는 기준(security). 카드정보는 저장하지 않는다.
 */
@Entity
@Table(name = "goods_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, length = 80)
    private String paymentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @Column(name = "goods_name", nullable = false, length = 200)
    private String goodsName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 40)
    private String receiverPhone;

    @Column(length = 20)
    private String zipcode;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(name = "address_detail", length = 300)
    private String addressDetail;

    @Column(length = 300)
    private String memo;

    @Column(name = "pg_tx_id", length = 120)
    private String pgTxId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private GoodsOrder(String paymentId, Long memberId, Long goodsId, String goodsName,
                       int quantity, int unitPrice, int totalAmount,
                       String receiverName, String receiverPhone, String zipcode,
                       String address, String addressDetail, String memo) {
        this.paymentId = paymentId;
        this.memberId = memberId;
        this.goodsId = goodsId;
        this.goodsName = goodsName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.memo = memo;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /** PG 결제 검증 완료. */
    public void markPaid(String pgTxId) {
        this.status = OrderStatus.PAID;
        this.pgTxId = pgTxId;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** 결제 실패/금액 불일치. (재고 환원은 서비스에서 처리) */
    public void markFailed() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
}
