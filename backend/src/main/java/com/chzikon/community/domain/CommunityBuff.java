package com.chzikon.community.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 버프(추천). UNIQUE(post_id, member_id) = 1인 1회, 다시 누르면 취소. */
@Entity
@Table(name = "community_buff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityBuff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public CommunityBuff(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
    }
}
