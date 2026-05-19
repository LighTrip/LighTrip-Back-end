-- 트리거 추가 이전 데이터 location 컬럼 backfill
-- > 트리거는 INSERT/UPDATE 시점에만 동작하므로, 이전 데이터는 NULL일 수 있음
UPDATE public.passport
SET location = ST_SetSRID(ST_MakePoint(longitude::float, latitude::float), 4326)
WHERE location IS NULL;

-- GiST 인덱스 추가
-- > ST_DWithin(피드 반경 필터), ST_Within(지도 bounding box 필터) 성능 최적화
-- > 기존 seq scan 대비 공간 쿼리 성능 대폭 개선
CREATE INDEX IF NOT EXISTS idx_passport_location ON public.passport USING GIST(location);
