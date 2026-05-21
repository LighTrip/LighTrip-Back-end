-- =============================================================================
-- LighTrip 더미 데이터 롤백 (Issue #78)
-- 목적     : dummy_seed.sql 로 삽입한 데이터 전체 제거
-- 식별자   : nickname LIKE 'dummy_user_%' 인 유저 + 그에 연결된 모든 데이터
-- 안전성   : 트랜잭션 + ON_ERROR_STOP 으로 실패 시 전체 롤백
-- 실행 방법:
--   psql -h <RDS_HOST> -U <USER> -d <DB> -v ON_ERROR_STOP=1 -f dummy_seed_rollback.sql
--
-- ⚠️ prod 환경에서 실행 시 사고 위험 — 반드시 RDS snapshot 후 실행
-- =============================================================================

BEGIN;

-- 삭제 전 카운트 출력
DO $$
DECLARE
    user_cnt bigint;
BEGIN
    SELECT COUNT(*) INTO user_cnt FROM users WHERE nickname LIKE 'dummy_user_%';
    RAISE NOTICE '롤백 대상 더미 유저 수: %', user_cnt;
END $$;

-- 자식 → 부모 순으로 삭제 (FK 제약 위반 방지)

-- 1. district_cover
DELETE FROM district_cover
WHERE user_id IN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%');

-- 2. scrap
DELETE FROM scrap
WHERE user_id IN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%')
   OR passport_id IN (
       SELECT p.passport_id
       FROM passport p
       JOIN users u ON p.user_id = u.user_id
       WHERE u.nickname LIKE 'dummy_user_%'
   );

-- 3. likes
DELETE FROM likes
WHERE user_id IN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%')
   OR passport_id IN (
       SELECT p.passport_id
       FROM passport p
       JOIN users u ON p.user_id = u.user_id
       WHERE u.nickname LIKE 'dummy_user_%'
   );

-- 4. passport_stamp
DELETE FROM passport_stamp
WHERE passport_id IN (
    SELECT p.passport_id
    FROM passport p
    JOIN users u ON p.user_id = u.user_id
    WHERE u.nickname LIKE 'dummy_user_%'
);

-- 5. passport_image
DELETE FROM passport_image
WHERE passport_id IN (
    SELECT p.passport_id
    FROM passport p
    JOIN users u ON p.user_id = u.user_id
    WHERE u.nickname LIKE 'dummy_user_%'
);

-- 6. passport
DELETE FROM passport
WHERE user_id IN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%');

-- 7. friend
DELETE FROM friend
WHERE request_id IN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%')
   OR receiver_id IN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%');

-- 8. users
DELETE FROM users WHERE nickname LIKE 'dummy_user_%';

-- 결과 확인
DO $$
DECLARE
    remaining bigint;
BEGIN
    SELECT COUNT(*) INTO remaining FROM users WHERE nickname LIKE 'dummy_user_%';
    IF remaining = 0 THEN
        RAISE NOTICE '✅ 모든 더미 데이터 삭제 완료';
    ELSE
        RAISE EXCEPTION '❌ 더미 유저 % 명이 남아 있음 - 롤백', remaining;
    END IF;
END $$;

COMMIT;
