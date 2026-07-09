package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 스트리머 개인 게시판 글.
 * 작성 = 로그인 회원 누구나 / 삭제 = 작성자 본인 + 해당 스트리머 + ADMIN (서비스에서 강제).
 */
@Entity
@Table(name = "streamer_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public StreamerPost(Long streamerId, Long authorId, String title, String content) {
        this.streamerId = streamerId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
