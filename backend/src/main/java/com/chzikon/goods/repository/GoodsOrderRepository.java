package com.chzikon.goods.repository;

import com.chzikon.goods.domain.GoodsOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoodsOrderRepository extends JpaRepository<GoodsOrder, Long> {

    Optional<GoodsOrder> findByPaymentId(String paymentId);

    List<GoodsOrder> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<GoodsOrder> findAllByOrderByCreatedAtDesc();
}
