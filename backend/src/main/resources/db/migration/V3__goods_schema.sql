-- =====================================================================
-- V3 굿즈/주문 스키마 — 치직온(CHZIKON) GOODS (Phase 5 MVP)
-- PG=PortOne(V2). 카드정보 미저장(PG 위임 = PCI 범위 회피).
-- 결제금액은 주문 시점 서버 스냅샷(total_amount)으로 PG 응답과 대조 = 위변조 검증.
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용 (ENGINE/CHARSET 절 없음)
-- =====================================================================

-- ---------- goods : 굿즈 상품 ----------
CREATE TABLE goods (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    image_url   VARCHAR(512),
    price       INT          NOT NULL,                -- 판매가(원, KRW). 0 이상
    stock       INT          NOT NULL DEFAULT 0,      -- 재고 수량 (0 = 품절)
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE(판매)/HIDDEN(숨김)
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL
);
CREATE INDEX idx_goods_status ON goods (status, sort_order);

-- ---------- goods_order : 주문 ----------
-- 단일 상품 주문(수량 지정). 다상품 장바구니는 다음 단계.
CREATE TABLE goods_order (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    payment_id     VARCHAR(80)  NOT NULL,             -- PortOne paymentId(가맹점 주문식별자). 유니크
    member_id      BIGINT       NOT NULL,
    goods_id       BIGINT       NOT NULL,
    goods_name     VARCHAR(200) NOT NULL,             -- 주문 시점 상품명 스냅샷
    quantity       INT          NOT NULL,
    unit_price     INT          NOT NULL,             -- 주문 시점 단가 스냅샷
    total_amount   INT          NOT NULL,             -- 서버 계산 결제금액(위변조 검증 기준)
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING/PAID/CANCELLED/FAILED
    receiver_name  VARCHAR(100) NOT NULL,
    receiver_phone VARCHAR(40)  NOT NULL,
    zipcode        VARCHAR(20),
    address        VARCHAR(300) NOT NULL,
    address_detail VARCHAR(300),
    memo           VARCHAR(300),
    pg_tx_id       VARCHAR(120),                      -- PortOne transactionId(결제 트랜잭션)
    paid_at        DATETIME,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    CONSTRAINT uq_goods_order_payment_id UNIQUE (payment_id),
    CONSTRAINT fk_goods_order_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_goods_order_goods  FOREIGN KEY (goods_id)  REFERENCES goods (id)
);
CREATE INDEX idx_goods_order_member ON goods_order (member_id, created_at);
CREATE INDEX idx_goods_order_status ON goods_order (status);
