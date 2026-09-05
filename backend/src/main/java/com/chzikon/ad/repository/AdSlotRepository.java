package com.chzikon.ad.repository;

import com.chzikon.ad.domain.AdSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdSlotRepository extends JpaRepository<AdSlot, Long> {
    List<AdSlot> findAllByOrderBySortOrderAscIdDesc();

    List<AdSlot> findByActiveTrueOrderBySortOrderAscIdDesc();
}
