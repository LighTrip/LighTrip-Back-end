-- V1에서 미사용 컬럼으로 생성된 like_count, rank 제거
-- 엔티티(Ranking)에 매핑이 없어 INSERT 시 NOT NULL 위반을 유발하던 레거시 컬럼
-- rank는 ranking_position과 중복, like_count는 미사용
ALTER TABLE public.ranking DROP COLUMN IF EXISTS like_count;
ALTER TABLE public.ranking DROP COLUMN IF EXISTS rank;
