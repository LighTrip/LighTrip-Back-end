-- V1에서 미사용 컬럼으로 생성된 invite_code 제거
ALTER TABLE public.team DROP COLUMN invite_code;
