-- 콜라보 게임 → 캠페인(후기 게시판) 연결. NULL 이면 미연결.
ALTER TABLE collab_game ADD COLUMN campaign_id BIGINT NULL;
