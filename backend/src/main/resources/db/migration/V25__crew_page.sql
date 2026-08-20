-- 크루 페이지 콘텐츠 서버 저장(2026-08-20) — /crew/<slug>/ 정적 HTML이 로드 시 GET,
-- 페이지 내 관리자(ADMIN)가 수정 후 PUT 하면 즉시 전 방문자에게 반영(HTML 파일 교체 불필요).
-- MEDIUMTEXT: 배너·작품 이미지가 data URI(base64)로 포함돼 TEXT(64KB) 초과 가능.
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
CREATE TABLE crew_page (
    slug       VARCHAR(100) PRIMARY KEY,
    data       MEDIUMTEXT   NOT NULL,
    updated_by BIGINT       NULL,
    updated_at DATETIME     NOT NULL
);
