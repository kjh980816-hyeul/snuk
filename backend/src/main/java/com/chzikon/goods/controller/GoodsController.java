package com.chzikon.goods.controller;

import com.chzikon.goods.dto.GoodsResponse;
import com.chzikon.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 굿즈 상점 — 공개 조회(ACTIVE 상품만). */
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @GetMapping
    public ResponseEntity<List<GoodsResponse>> list() {
        return ResponseEntity.ok(goodsService.listActive().stream()
                .map(GoodsResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(GoodsResponse.from(goodsService.getById(id)));
    }
}
