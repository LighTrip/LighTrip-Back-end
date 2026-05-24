-- district_cover에서 passport_image_id 외래키를 제거하고 image_url을 직접 저장하도록 전환
-- > 변경 이유: 사용자가 여권 이미지 외에도 자유로운 이미지 URL을 커버로 지정할 수 있도록 유연성 확보.
-- > 데이터 보존: 기존 passport_image_id가 가리키던 image_url을 backfill로 옮긴 뒤 컬럼 삭제.

-- 1. 새 image_url 컬럼 추가 (일단 nullable로)
ALTER TABLE public.district_cover
    ADD COLUMN image_url TEXT;

-- 2. 기존 passport_image_id 기반으로 image_url 채움
UPDATE public.district_cover dc
SET image_url = pi.image_url
FROM public.passport_image pi
WHERE dc.passport_image_id = pi.passport_image_id;

-- 3. NOT NULL 제약 추가
ALTER TABLE public.district_cover
    ALTER COLUMN image_url SET NOT NULL;

-- 4. 기존 passport_image_id 컬럼 + FK 제약 제거 (CASCADE로 의존 제약 자동 삭제)
ALTER TABLE public.district_cover
    DROP COLUMN passport_image_id;
