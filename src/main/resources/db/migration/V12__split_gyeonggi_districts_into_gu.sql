-- 경기도 구가 있는 8개 시(수원·성남·고양·용인·부천·안산·안양·화성)를 구 단위로 분리.
-- 기존 시 단위 기록은 대표 구(시청 소재지)로 이전한다. 데이터가 없으면 UPDATE는 no-op.
-- district_category의 CHECK 제약은 재생성하지 않는다(V13에서 제거되어 enum이 단일 진실 공급원이 됨).

-- 1) 기존 CHECK 제약 제거 (새 구 값으로 UPDATE 하려면 먼저 풀어야 함)
ALTER TABLE public.passport DROP CONSTRAINT IF EXISTS passport_district_category_check;
ALTER TABLE public.district_cover DROP CONSTRAINT IF EXISTS district_cover_district_category_check;

-- 2) 기존 시 단위 데이터를 대표 구로 이전
UPDATE public.passport SET district_category = CASE district_category
    WHEN 'SUWON'    THEN 'SUWON_PALDAL'
    WHEN 'SEONGNAM' THEN 'SEONGNAM_JUNGWON'
    WHEN 'GOYANG'   THEN 'GOYANG_DEOKYANG'
    WHEN 'YONGIN'   THEN 'YONGIN_CHEOIN'
    WHEN 'BUCHEON'  THEN 'BUCHEON_WONMI'
    WHEN 'ANSAN'    THEN 'ANSAN_DANWON'
    WHEN 'ANYANG'   THEN 'ANYANG_DONGAN'
    WHEN 'HWASEONG' THEN 'HWASEONG_DONGTAN'
    ELSE district_category END
WHERE district_category IN ('SUWON','SEONGNAM','GOYANG','YONGIN','BUCHEON','ANSAN','ANYANG','HWASEONG');

UPDATE public.district_cover SET district_category = CASE district_category
    WHEN 'SUWON'    THEN 'SUWON_PALDAL'
    WHEN 'SEONGNAM' THEN 'SEONGNAM_JUNGWON'
    WHEN 'GOYANG'   THEN 'GOYANG_DEOKYANG'
    WHEN 'YONGIN'   THEN 'YONGIN_CHEOIN'
    WHEN 'BUCHEON'  THEN 'BUCHEON_WONMI'
    WHEN 'ANSAN'    THEN 'ANSAN_DANWON'
    WHEN 'ANYANG'   THEN 'ANYANG_DONGAN'
    WHEN 'HWASEONG' THEN 'HWASEONG_DONGTAN'
    ELSE district_category END
WHERE district_category IN ('SUWON','SEONGNAM','GOYANG','YONGIN','BUCHEON','ANSAN','ANYANG','HWASEONG');
