-- 광고 슬롯 (2026-09-05): 홈 상단 AD 배너 전용. 이미지+링크+노출 on/off+기간+순서. 어드민 CRUD, 조회 공개.
CREATE TABLE ad_slot (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(100),
    image_url  VARCHAR(500) NOT NULL,
    link_url   VARCHAR(500),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order INT          NOT NULL DEFAULT 0,
    start_at   DATETIME,
    end_at     DATETIME,
    created_by BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL
);
CREATE INDEX idx_ad_slot_list ON ad_slot (is_active, sort_order);
