-- 전국 시·군·구 확장(약 255개)으로 District enum 항목이 크게 늘어남.
-- district_category에 모든 값을 CHECK로 나열하는 것은 유지보수가 어렵고,
-- 애플리케이션 레벨에서 JPA @Enumerated(enum)가 이미 유효성을 보장하므로 CHECK 제약을 제거한다.
-- 이후 지역 추가는 enum 수정만으로 가능(별도 마이그레이션 불필요).
ALTER TABLE public.passport DROP CONSTRAINT IF EXISTS passport_district_category_check;
ALTER TABLE public.district_cover DROP CONSTRAINT IF EXISTS district_cover_district_category_check;
