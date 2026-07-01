package com.chzikon.goods.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.util.ExternalUrlValidator;
import com.chzikon.goods.domain.Goods;
import com.chzikon.goods.domain.GoodsStatus;
import com.chzikon.goods.dto.GoodsRequest;
import com.chzikon.goods.repository.GoodsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final ExternalUrlValidator urlValidator;
    private final AdminLogService adminLogService;

    // ===== 공개 조회 =====

    /** 상점 노출 = ACTIVE 만. */
    @Transactional(readOnly = true)
    public List<Goods> listActive() {
        return goodsRepository.findByStatusOrderBySortOrderAscIdDesc(GoodsStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Goods getById(Long id) {
        return goodsRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    // ===== 어드민 =====

    @Transactional(readOnly = true)
    public List<Goods> listAll() {
        return goodsRepository.findAllByOrderBySortOrderAscIdDesc();
    }

    @Transactional
    public Goods create(GoodsRequest req, Long actorId) {
        urlValidator.validateNullable(req.imageUrl());
        Goods saved = goodsRepository.save(req.toEntity());
        adminLogService.record(actorId, "GOODS_CREATE", "goods", saved.getId(),
                "name=" + saved.getName() + " price=" + saved.getPrice() + " stock=" + saved.getStock());
        return saved;
    }

    @Transactional
    public Goods update(Long id, GoodsRequest req, Long actorId) {
        urlValidator.validateNullable(req.imageUrl());
        Goods goods = getById(id);
        goods.update(req.name(), req.description(), req.imageUrl(), req.price(),
                req.stock(), req.status(), req.sortOrder());
        adminLogService.record(actorId, "GOODS_UPDATE", "goods", id,
                "price=" + goods.getPrice() + " stock=" + goods.getStock() + " status=" + goods.getStatus());
        return goods;
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Goods goods = getById(id);
        goodsRepository.delete(goods);
        adminLogService.record(actorId, "GOODS_DELETE", "goods", id, "name=" + goods.getName());
    }
}
