package com.chzikon.goods.repository;

import com.chzikon.goods.domain.Goods;
import com.chzikon.goods.domain.GoodsStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    List<Goods> findAllByOrderBySortOrderAscIdDesc();

    List<Goods> findByStatusOrderBySortOrderAscIdDesc(GoodsStatus status);

    /** 재고 선점/환원용 비관적 쓰기 락 — 동시 주문 시 재고 초과판매 방지. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goods g where g.id = :id")
    Optional<Goods> findByIdForUpdate(@Param("id") Long id);
}
