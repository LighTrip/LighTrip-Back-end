BEGIN;

-- 재현 가능한 랜덤 시드
SELECT setseed(0.42);


INSERT INTO users (nickname, email, profile_img, friend_code, location, bio,
                   theme_color, passport_theme, created_at, updated_at)
SELECT
    'dummy_user_' || LPAD(gs::text, 3, '0'),
    'dummy' || gs || '@lightrip.dev',
    'https://cdn.lightrip.cloud/profile-images/dummy/profile_' || gs || '.jpg',
    'D' || LPAD(UPPER(TO_HEX(gs)), 7, '0'),
    CASE (gs % 3)
        WHEN 0 THEN '서울특별시'
        WHEN 1 THEN '경기도'
        ELSE '인천광역시'
    END,
    '안녕하세요! 더미 유저 ' || gs || '입니다.',
    (ARRAY['#FFFFFF','#FFCCCC','#CCFFCC','#CCCCFF','#FFFFCC','#CCFFFF','#FFCCFF'])[1 + (gs % 7)],
    CASE WHEN gs % 4 = 0 THEN 'classic' ELSE NULL END,
    NOW() - (random() * INTERVAL '180 days'),
    NOW()
FROM generate_series(1, 300) AS gs;

-- =============================================================================
-- 2. FRIENDS (500건) - 70% ACCEPTED, 30% PENDING
-- =============================================================================
INSERT INTO friend (request_id, receiver_id, status, created_at)
WITH dummy_users AS (
    SELECT user_id
    FROM users
    WHERE nickname LIKE 'dummy_user_%'
),
pairs AS (
    SELECT
        a.user_id AS request_id,
        b.user_id AS receiver_id,
        random() AS r
    FROM dummy_users a
    CROSS JOIN dummy_users b
    WHERE a.user_id < b.user_id
    ORDER BY r
    LIMIT 1500
)
SELECT
    request_id,
    receiver_id,
    CASE WHEN random() < 0.7 THEN 'ACCEPTED' ELSE 'PENDING' END,
    NOW() - (random() * INTERVAL '90 days')
FROM pairs;

-- =============================================================================
-- 3. PASSPORTS (~10000건 시도, 충돌 시 ON CONFLICT) - 300명 × 평균 33건
-- =============================================================================
INSERT INTO passport (
    user_id, content, latitude, longitude, address, visited_at,
    district_category, category, visibility,
    like_count, scrap_count, ai_category, space_name,
    music_title, music_artist, created_at, updated_at
)
WITH dummy_users AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY user_id) AS rn
    FROM users
    WHERE nickname LIKE 'dummy_user_%'
),
districts(arr) AS (VALUES (ARRAY[
    'JONGNO','JUNG','YONGSAN','SEONGDONG','GWANGJIN','DONGDAEMUN',
    'JUNGNANG','SEONGBUK','GANGBUK','DOBONG','NOWON','EUNPYEONG',
    'SEODAEMUN','MAPO','YANGCHEON','GANGSEO','GURO','GEUMCHEON',
    'YEONGDEUNGPO','DONGJAK','GWANAK','SEOCHO','GANGNAM','SONGPA','GANGDONG',
    'SUWON','SEONGNAM','GOYANG','YONGIN','BUCHEON','ANSAN','ANYANG',
    'NAMYANGJU','HWASEONG','PYEONGTAEK','UIJEONGBU','SIHEUNG','PAJU',
    'GIMPO','GWANGMYEONG','GWANGJU_GG','GUNPO','HANAM','OSAN','ICHEON',
    'YANGJU','ANSEONG','GURI','POCHEON','UIWANG','YEOJU','DONGDUCHEON',
    'GAPYEONG','YANGPYEONG','YEONCHEON','GWACHEON'
])),
categories(arr) AS (VALUES (ARRAY[
    'CAFE','RESTAURANT','BAR','CULTURE','ACTIVITY','SHOPPING','NATURE','ETC'
])),
visibilities(arr) AS (VALUES (ARRAY[
    'PUBLIC','PUBLIC','PUBLIC','PUBLIC','PUBLIC','PUBLIC','PUBLIC',
    'FRIENDS_ONLY','FRIENDS_ONLY','PRIVATE'
]))
SELECT
    du.user_id,
    '여기 정말 좋았어요! 다시 방문하고 싶은 곳입니다. (더미 여권 #' || gs || ')',
    (37.4 + random() * 0.4)::numeric(10,7),
    (126.8 + random() * 0.5)::numeric(10,7),
    '서울특별시 더미구 더미로 ' || gs,
    CURRENT_DATE - ((random() * 180)::int),
    d.arr[1 + (random() * (array_length(d.arr, 1) - 1))::int],
    c.arr[1 + (random() * (array_length(c.arr, 1) - 1))::int],
    v.arr[1 + (random() * (array_length(v.arr, 1) - 1))::int],
    (random() * 100)::bigint,
    (random() * 30)::bigint,
    c.arr[1 + (random() * (array_length(c.arr, 1) - 1))::int],
    '더미 장소 #' || gs,
    'Dummy Song #' || ((gs % 100) + 1),
    'Dummy Artist #' || ((gs % 50) + 1),
    NOW() - (random() * INTERVAL '180 days'),
    NOW()
FROM generate_series(1, 10000) AS gs
CROSS JOIN districts d
CROSS JOIN categories c
CROSS JOIN visibilities v
JOIN dummy_users du ON du.rn = ((gs - 1) % 300) + 1
ON CONFLICT (user_id, latitude, longitude, visited_at) DO NOTHING;

-- =============================================================================
-- 4. PASSPORT_IMAGE (여권당 1~5장)
-- =============================================================================
INSERT INTO passport_image (passport_id, image_order, image_url, created_at)
SELECT
    p.passport_id,
    img_order,
    'https://cdn.lightrip.cloud/passport-images/dummy/' || p.passport_id || '_' || img_order || '.jpg',
    p.created_at
FROM passport p
JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%'
CROSS JOIN LATERAL generate_series(1, 1 + (random() * 4)::int) AS img_order;

-- =============================================================================
-- 5. PASSPORT_STAMP (여권당 1~2개 카테고리)
-- =============================================================================

-- 5-1. 기본 스탬프: 여권의 main category 스탬프 1개
INSERT INTO passport_stamp (passport_id, category, image_url, created_at)
SELECT
    p.passport_id,
    p.category,
    'https://cdn.lightrip.cloud/stamps/' || p.category || '.png',
    p.created_at
FROM passport p
JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';

-- 5-2. 추가 스탬프: 30% 여권에 보조 카테고리 1개 더
INSERT INTO passport_stamp (passport_id, category, image_url, created_at)
SELECT
    p.passport_id,
    c.extra_cat::varchar,
    'https://cdn.lightrip.cloud/stamps/' || c.extra_cat || '.png',
    p.created_at
FROM passport p
JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%'
CROSS JOIN LATERAL (
    SELECT (ARRAY['CAFE','RESTAURANT','BAR','CULTURE','ACTIVITY','SHOPPING','NATURE','ETC'])
           [1 + (random() * 7)::int] AS extra_cat
) c
WHERE random() < 0.3 AND c.extra_cat <> p.category::text
ON CONFLICT (passport_id, category) DO NOTHING;

-- =============================================================================
-- 6. LIKES (~8000건) - 다른 유저가 PUBLIC 여권에 좋아요
-- =============================================================================
INSERT INTO likes (user_id, passport_id, created_at)
SELECT
    u.user_id,
    p.passport_id,
    NOW() - (random() * INTERVAL '60 days')
FROM (
    SELECT user_id
    FROM users
    WHERE nickname LIKE 'dummy_user_%'
) u
CROSS JOIN LATERAL (
    SELECT passport_id
    FROM passport pp
    JOIN users pu ON pp.user_id = pu.user_id AND pu.nickname LIKE 'dummy_user_%'
    WHERE pp.visibility = 'PUBLIC' AND pp.user_id <> u.user_id
    ORDER BY random()
    LIMIT 60
) p
ON CONFLICT (user_id, passport_id) DO NOTHING;

-- =============================================================================
-- 7. SCRAP (~4000건) - 다른 유저가 PUBLIC 여권 스크랩
-- =============================================================================
INSERT INTO scrap (user_id, passport_id, created_at)
SELECT
    u.user_id,
    p.passport_id,
    NOW() - (random() * INTERVAL '60 days')
FROM (
    SELECT user_id
    FROM users
    WHERE nickname LIKE 'dummy_user_%'
) u
CROSS JOIN LATERAL (
    SELECT passport_id
    FROM passport pp
    JOIN users pu ON pp.user_id = pu.user_id AND pu.nickname LIKE 'dummy_user_%'
    WHERE pp.visibility = 'PUBLIC' AND pp.user_id <> u.user_id
    ORDER BY random()
    LIMIT 30
) p
ON CONFLICT (user_id, passport_id) DO NOTHING;

-- =============================================================================
-- 8. like_count / scrap_count 동기화
-- =============================================================================
UPDATE passport p
SET like_count = COALESCE(l.cnt, 0)
FROM (
    SELECT passport_id, COUNT(*)::bigint AS cnt
    FROM likes
    GROUP BY passport_id
) l
WHERE p.passport_id = l.passport_id;

UPDATE passport p
SET scrap_count = COALESCE(s.cnt, 0)
FROM (
    SELECT passport_id, COUNT(*)::bigint AS cnt
    FROM scrap
    GROUP BY passport_id
) s
WHERE p.passport_id = s.passport_id;

-- =============================================================================
-- 9. DISTRICT_COVER (유저별 지역 대표 이미지)
--    각 (user, district)별 가장 최근 여권의 첫 이미지를 cover 로 지정
-- =============================================================================
INSERT INTO district_cover (user_id, district_category, image_url, text_color, created_at, updated_at)
SELECT DISTINCT ON (p.user_id, p.district_category)
    p.user_id,
    p.district_category,
    pi.image_url,
    (ARRAY['#FFFFFF','#000000','#333333','#666666'])[1 + (random() * 3)::int],
    NOW(),
    NOW()
FROM passport p
JOIN passport_image pi ON pi.passport_id = p.passport_id AND pi.image_order = 1
JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%'
ORDER BY p.user_id, p.district_category, p.created_at DESC
ON CONFLICT (user_id, district_category) DO NOTHING;

-- =============================================================================
-- 10. TEAM (20개)
-- =============================================================================
INSERT INTO team (team_name, team_code, created_at, updated_at)
SELECT
    '더미팀_' || LPAD(gs::text, 3, '0'),
    'TEAM' || LPAD(gs::text, 4, '0'),
    NOW() - (random() * INTERVAL '90 days'),
    NOW()
FROM generate_series(1, 20) AS gs
ON CONFLICT (team_code) DO NOTHING;

-- =============================================================================
-- 11. TEAM_MEMBER (팀당 3~5명, 첫 사람은 LEADER 나머지는 MEMBER)
-- =============================================================================
INSERT INTO team_member (team_id, user_id, role, joined_at)
WITH dummy_teams AS (
    SELECT
        team_id,
        3 + (random() * 2)::int AS team_size
    FROM team WHERE team_name LIKE '더미팀_%'
),
team_users AS (
    SELECT
        dt.team_id,
        dt.team_size,
        u.user_id,
        ROW_NUMBER() OVER (PARTITION BY dt.team_id ORDER BY random()) AS rn
    FROM dummy_teams dt
    CROSS JOIN (SELECT user_id FROM users WHERE nickname LIKE 'dummy_user_%') u
)
SELECT
    team_id,
    user_id,
    CASE WHEN rn = 1 THEN 'LEADER' ELSE 'MEMBER' END,
    NOW() - (random() * INTERVAL '60 days')
FROM team_users
WHERE rn <= team_size;

-- =============================================================================
-- 결과 요약
-- =============================================================================
DO $$
DECLARE
    user_cnt    bigint;
    friend_cnt  bigint;
    passport_cnt bigint;
    image_cnt   bigint;
    stamp_cnt   bigint;
    like_cnt    bigint;
    scrap_cnt   bigint;
    cover_cnt   bigint;
    team_cnt    bigint;
    team_member_cnt bigint;
BEGIN
    SELECT COUNT(*) INTO user_cnt FROM users WHERE nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO friend_cnt FROM friend f
        JOIN users u1 ON f.request_id = u1.user_id AND u1.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO passport_cnt FROM passport p
        JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO image_cnt FROM passport_image pi
        JOIN passport p ON pi.passport_id = p.passport_id
        JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO stamp_cnt FROM passport_stamp ps
        JOIN passport p ON ps.passport_id = p.passport_id
        JOIN users u ON p.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO like_cnt FROM likes l
        JOIN users u ON l.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO scrap_cnt FROM scrap s
        JOIN users u ON s.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO cover_cnt FROM district_cover dc
        JOIN users u ON dc.user_id = u.user_id AND u.nickname LIKE 'dummy_user_%';
    SELECT COUNT(*) INTO team_cnt FROM team WHERE team_name LIKE '더미팀_%';
    SELECT COUNT(*) INTO team_member_cnt FROM team_member tm
        JOIN team t ON tm.team_id = t.team_id AND t.team_name LIKE '더미팀_%';

    RAISE NOTICE '=== LighTrip 더미 데이터 삽입 결과 ===';
    RAISE NOTICE 'users          : %', user_cnt;
    RAISE NOTICE 'friend         : %', friend_cnt;
    RAISE NOTICE 'passport       : %', passport_cnt;
    RAISE NOTICE 'passport_image : %', image_cnt;
    RAISE NOTICE 'passport_stamp : %', stamp_cnt;
    RAISE NOTICE 'likes          : %', like_cnt;
    RAISE NOTICE 'scrap          : %', scrap_cnt;
    RAISE NOTICE 'district_cover : %', cover_cnt;
    RAISE NOTICE 'team           : %', team_cnt;
    RAISE NOTICE 'team_member    : %', team_member_cnt;
END $$;

COMMIT;