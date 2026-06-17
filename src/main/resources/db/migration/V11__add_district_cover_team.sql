-- district_cover에 팀 차원 추가: team_id가 NULL이면 개인 커버, 값이 있으면 팀 커버
-- 팀 커버는 user_id = NULL, team_id = 값으로 저장 → 기존 user 기준 쿼리는 개인 커버만 조회됨
ALTER TABLE public.district_cover ADD COLUMN team_id bigint;

-- 팀 커버는 작성자(user_id)를 비우므로 NOT NULL 제거
ALTER TABLE public.district_cover ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE public.district_cover
    ADD CONSTRAINT fk_district_cover_team FOREIGN KEY (team_id) REFERENCES public.team(team_id);

-- 기존 (user_id, district_category) 전체 유니크 제약을 제거하고, 개인/팀 부분 유니크 인덱스로 분리
ALTER TABLE public.district_cover DROP CONSTRAINT uk_district_cover_user_district;

CREATE UNIQUE INDEX uk_district_cover_user_district
    ON public.district_cover (user_id, district_category)
    WHERE team_id IS NULL;

CREATE UNIQUE INDEX uk_district_cover_team_district
    ON public.district_cover (team_id, district_category)
    WHERE team_id IS NOT NULL;
