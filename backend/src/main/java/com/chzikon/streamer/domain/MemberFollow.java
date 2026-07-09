package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 회원 → 스트리머 팔로우. UNIQUE(follower_id, streamer_id). */
@Entity
@Table(name = "member_follow")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MemberFollow(Long followerId, Long streamerId) {
        this.followerId = followerId;
        this.streamerId = streamerId;
        this.createdAt = LocalDateTime.now();
    }
}
