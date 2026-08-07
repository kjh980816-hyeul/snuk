package com.chzikon.community.domain;

import com.chzikon.member.domain.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 커뮤니티 게시판. parentId = null 이면 상위 그룹(예: 정보공유), 아니면 하위 게시판(예: 정보공유 › 방송).
 * writeRole/readRole = 어드민이 게시판마다 지정하는 최소 등급(VIEWER=로그인 회원, GUEST=누구나).
 * source = 시스템 게시판 연결(NOTICE=공지, NEWS=스눅 뉴스). null 이면 일반 커뮤니티 글.
 */
@Entity
@Table(name = "community_board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "write_role", nullable = false, length = 20)
    private Role writeRole = Role.VIEWER;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_role", nullable = false, length = 20)
    private Role readRole = Role.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BoardSource source;

    public CommunityBoard(Long parentId, String name, int sortOrder, boolean visible,
                          Role writeRole, Role readRole) {
        this.parentId = parentId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.visible = visible;
        this.writeRole = writeRole != null ? writeRole : Role.VIEWER;
        this.readRole = readRole != null ? readRole : Role.GUEST;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, Integer sortOrder, Boolean visible, Role writeRole, Role readRole) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (visible != null) this.visible = visible;
        if (writeRole != null) this.writeRole = writeRole;
        if (readRole != null) this.readRole = readRole;
    }

    public boolean isGroup() {
        return parentId == null;
    }

    /** 공지·뉴스처럼 다른 테이블을 창구만 커뮤니티로 쓰는 게시판. */
    public boolean isSystem() {
        return source != null;
    }
}
