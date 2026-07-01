package com.chzikon.collab.controller;

import com.chzikon.collab.dto.CollabDtos.*;
import com.chzikon.collab.service.CollabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 콜라보/노출 — 공개 조회(홈). */
@RestController
@RequestMapping("/api/collab")
@RequiredArgsConstructor
public class CollabController {

    private final CollabService collabService;

    @GetMapping("/games")
    public ResponseEntity<List<CollabGameResponse>> games() {
        return ResponseEntity.ok(collabService.games().stream().map(CollabGameResponse::from).toList());
    }

    @GetMapping("/videos")
    public ResponseEntity<List<ContentVideoResponse>> videos() {
        return ResponseEntity.ok(collabService.videos().stream().map(ContentVideoResponse::from).toList());
    }

    @GetMapping("/clients")
    public ResponseEntity<List<ClientLogoResponse>> clients() {
        return ResponseEntity.ok(collabService.logos().stream().map(ClientLogoResponse::from).toList());
    }
}
