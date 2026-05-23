-- district_cover에 텍스트 색상(HEX) 컬럼 추가
-- > 여권 커버 이미지 위에 표시되는 지역명 텍스트의 색상.
-- > 프론트엔드 UI에서 #RRGGBB 형식 HEX 코드 문자열로 사용.
-- > 기본값은 흰색(#FFFFFF). 기존 레코드는 DEFAULT로 자동 채워짐.
ALTER TABLE public.district_cover
    ADD COLUMN text_color VARCHAR(7) NOT NULL DEFAULT '#FFFFFF';
