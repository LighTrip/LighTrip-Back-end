ALTER TABLE public.auth DROP CONSTRAINT auth_social_type_check;

ALTER TABLE public.auth ADD CONSTRAINT auth_social_type_check
    CHECK (((social_type)::text = ANY ((ARRAY['KAKAO'::character varying, 'GOOGLE'::character varying, 'APPLE'::character varying])::text[])));
