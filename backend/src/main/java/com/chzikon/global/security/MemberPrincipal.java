package com.chzikon.global.security;

import com.chzikon.member.domain.Role;

/**
 * SecurityContext 에 담기는 인증 주체. 컨트롤러에서 @AuthenticationPrincipal 로 주입.
 */
public record MemberPrincipal(Long memberId, Role role) {
}
