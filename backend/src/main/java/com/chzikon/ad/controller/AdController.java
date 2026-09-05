package com.chzikon.ad.controller;

import com.chzikon.ad.dto.AdSlotDtos.AdSlotResponse;
import com.chzikon.ad.service.AdSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 홈 AD 배너 — 공개(SecurityConfig permitAll). 지금 노출 중인 슬롯만. */
@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdSlotService adSlotService;

    @GetMapping
    public ResponseEntity<List<AdSlotResponse>> live() {
        return ResponseEntity.ok(adSlotService.findLive().stream().map(AdSlotResponse::from).toList());
    }
}
