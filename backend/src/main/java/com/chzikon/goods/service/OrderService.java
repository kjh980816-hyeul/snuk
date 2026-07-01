package com.chzikon.goods.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.goods.client.PortoneClient;
import com.chzikon.goods.client.PortonePayment;
import com.chzikon.goods.config.PortoneProperties;
import com.chzikon.goods.domain.Goods;
import com.chzikon.goods.domain.GoodsOrder;
import com.chzikon.goods.dto.OrderCreateRequest;
import com.chzikon.goods.repository.GoodsOrderRepository;
import com.chzikon.goods.repository.GoodsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 굿즈 주문/결제. 보안 원칙:
 * - 결제금액은 서버가 상품가×수량으로 확정(total_amount 스냅샷). 프론트가 보낸 금액 불신뢰.
 * - 결제 완료 판정은 프론트 통보가 아니라 서버→PortOne 재조회 결과로만. 금액 일치까지 확인해야 PAID.
 * - 재고는 주문 생성 시 비관적 락으로 선점, 실패/취소 시 환원(초과판매 방지).
 * ⚠️ 미결제로 방치된 PENDING 주문의 재고 자동 환원(정리 스케줄러)은 다음 단계.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final GoodsRepository goodsRepository;
    private final GoodsOrderRepository orderRepository;
    private final PortoneClient portoneClient;
    private final PortoneProperties portoneProperties;
    private final AdminLogService adminLogService;

    /** 주문 생성 = 재고 선점 + PENDING 저장. 반환값으로 프론트가 PortOne 결제창 호출. */
    @Transactional
    public GoodsOrder create(OrderCreateRequest req, Long memberId) {
        // 재고 경쟁 자원은 락을 잡고 재확인
        Goods goods = goodsRepository.findByIdForUpdate(req.goodsId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!goods.isPurchasable()) {
            throw new BusinessException(ErrorCode.GOODS_NOT_AVAILABLE);
        }
        long total = (long) goods.getPrice() * req.quantity();
        if (total <= 0 || total > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "주문 금액이 올바르지 않습니다.");
        }

        goods.reserveStock(req.quantity()); // 부족 시 OUT_OF_STOCK

        GoodsOrder order = GoodsOrder.builder()
                .paymentId("snuk_" + UUID.randomUUID().toString().replace("-", ""))
                .memberId(memberId)
                .goodsId(goods.getId())
                .goodsName(goods.getName())
                .quantity(req.quantity())
                .unitPrice(goods.getPrice())
                .totalAmount((int) total)
                .receiverName(req.receiverName())
                .receiverPhone(req.receiverPhone())
                .zipcode(req.zipcode())
                .address(req.address())
                .addressDetail(req.addressDetail())
                .memo(req.memo())
                .build();
        return orderRepository.save(order);
    }

    /**
     * 결제 검증/반영 (멱등). 클라이언트 confirm 과 PortOne 웹훅 모두 이 경로로 수렴.
     * PortOne 재조회 → 결제완료 & 금액 일치 시에만 PAID. 금액 불일치는 실패 처리 + 재고 환원.
     */
    @Transactional
    public GoodsOrder reconcile(String paymentId) {
        GoodsOrder order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (order.isPaid() || !order.isPending()) {
            return order; // 이미 확정된 주문 — 재실행 무시(멱등)
        }

        PortonePayment payment = portoneClient.getPayment(paymentId); // 실패 시 PAYMENT_VERIFY_FAILED

        if (payment.isPaid()) {
            if (payment.totalAmount() == order.getTotalAmount()) {
                order.markPaid(payment.transactionId());
                adminLogService.record(order.getMemberId(), "ORDER_PAID", "goods_order", order.getId(),
                        "amount=" + order.getTotalAmount() + " tx=" + payment.transactionId());
            } else {
                // 금액 위변조/불일치 — 실패 처리 + 재고 환원 + 감사 로그(중요)
                failAndRestore(order);
                adminLogService.record(order.getMemberId(), "ORDER_AMOUNT_MISMATCH", "goods_order", order.getId(),
                        "expected=" + order.getTotalAmount() + " actual=" + payment.totalAmount());
            }
        } else if (payment.isFailedOrCancelled()) {
            failAndRestore(order);
        }
        // 그 외(결제 진행 중)는 PENDING 유지
        return order;
    }

    private void failAndRestore(GoodsOrder order) {
        goodsRepository.findByIdForUpdate(order.getGoodsId())
                .ifPresent(g -> g.restoreStock(order.getQuantity()));
        order.markFailed();
    }

    // ===== 조회 =====

    @Transactional(readOnly = true)
    public List<GoodsOrder> myOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public GoodsOrder getOwned(Long orderId, Long memberId) {
        GoodsOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!order.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OWNED);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<GoodsOrder> listAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public PortoneProperties portoneProperties() {
        return portoneProperties;
    }
}
