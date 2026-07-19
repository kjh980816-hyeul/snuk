-- 대회 등록 스트리머(NULL = 어드민 등록 = 스눅 공식). 본인 수정/삭제 권한 판별용.
ALTER TABLE tournament ADD COLUMN owner_member_id BIGINT;
