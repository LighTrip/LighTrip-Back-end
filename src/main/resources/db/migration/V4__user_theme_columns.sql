-- users 테이블에서 current_mode 컬럼 제거 + 테마 관련 컬럼 추가
-- > current_mode: 더 이상 사용하지 않는 사용자 상태(INDIVIDUAL/TEAM) 컬럼 제거.
-- > theme_color: 프로필 테마 색상 (HEX #RRGGBB). 기본값 흰색.
-- > passport_theme: 여권 테마. 테마 이름 또는 이미지 URL 등 자유 문자열. nullable.

ALTER TABLE public.users DROP COLUMN IF EXISTS current_mode;

ALTER TABLE public.users
    ADD COLUMN theme_color VARCHAR(7) NOT NULL DEFAULT '#FFFFFF';

ALTER TABLE public.users
    ADD COLUMN passport_theme TEXT;
