package com.chzikon.member.controller;

import com.chzikon.member.domain.Role;
import com.chzikon.member.dto.StreamerPublicView;
import com.chzikon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/streamers")
@RequiredArgsConstructor
public class StreamerController {

    private final MemberRepository memberRepository;

    /** 공식 파트너 스트리머(STREAMER 등급) — 공개, 팔로워순 상위 60명. */
    @GetMapping
    public ResponseEntity<List<StreamerPublicView>> list() {
        return ResponseEntity.ok(memberRepository.findTop60ByRoleOrderByFollowerCountDesc(Role.STREAMER)
                .stream().map(StreamerPublicView::from).toList());
    }
}
