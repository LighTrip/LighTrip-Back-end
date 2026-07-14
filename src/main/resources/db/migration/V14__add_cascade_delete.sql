-- 회원탈퇴 시 users 삭제가 FK 위반으로 실패하는 문제 수정 (Issue #177)
-- users(user_id)를 참조하는 모든 FK와 passport(passport_id)를 참조하는 FK에 ON DELETE CASCADE 추가

-- =============================================
-- 1. users 참조 FK → ON DELETE CASCADE
-- =============================================

-- auth
ALTER TABLE ONLY public.auth
    DROP CONSTRAINT fkpv45mvdt7km1gvrtn5f9rsvd7;
ALTER TABLE ONLY public.auth
    ADD CONSTRAINT fkpv45mvdt7km1gvrtn5f9rsvd7 FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- ranking
ALTER TABLE ONLY public.ranking
    DROP CONSTRAINT fk3fvvh22u2cwktrkpu73la05yp;
ALTER TABLE ONLY public.ranking
    ADD CONSTRAINT fk3fvvh22u2cwktrkpu73la05yp FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- friend (요청자 / 수신자 모두)
ALTER TABLE ONLY public.friend
    DROP CONSTRAINT fk3t89853ilx0np8f323suufvty;
ALTER TABLE ONLY public.friend
    ADD CONSTRAINT fk3t89853ilx0np8f323suufvty FOREIGN KEY (request_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.friend
    DROP CONSTRAINT fknbki3da2tox3pedxa9qs2gaqi;
ALTER TABLE ONLY public.friend
    ADD CONSTRAINT fknbki3da2tox3pedxa9qs2gaqi FOREIGN KEY (receiver_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- passport
ALTER TABLE ONLY public.passport
    DROP CONSTRAINT fkimp0xtvk5mujh4f1bkh7833vx;
ALTER TABLE ONLY public.passport
    ADD CONSTRAINT fkimp0xtvk5mujh4f1bkh7833vx FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- likes
ALTER TABLE ONLY public.likes
    DROP CONSTRAINT fknvx9seeqqyy71bij291pwiwrg;
ALTER TABLE ONLY public.likes
    ADD CONSTRAINT fknvx9seeqqyy71bij291pwiwrg FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- scrap
ALTER TABLE ONLY public.scrap
    DROP CONSTRAINT fk6jcivlvmiwrx9hd1d7h2tkije;
ALTER TABLE ONLY public.scrap
    ADD CONSTRAINT fk6jcivlvmiwrx9hd1d7h2tkije FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- district_cover
ALTER TABLE ONLY public.district_cover
    DROP CONSTRAINT fk9gi6fa8gt7hxcvl08ta7pr4ll;
ALTER TABLE ONLY public.district_cover
    ADD CONSTRAINT fk9gi6fa8gt7hxcvl08ta7pr4ll FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- team_member
ALTER TABLE ONLY public.team_member
    DROP CONSTRAINT fkcwlc6ivvbf5svve5fmbg71vks;
ALTER TABLE ONLY public.team_member
    ADD CONSTRAINT fkcwlc6ivvbf5svve5fmbg71vks FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- payment
ALTER TABLE ONLY public.payment
    DROP CONSTRAINT fk_payment_user;
ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

-- =============================================
-- 2. passport 참조 FK → ON DELETE CASCADE
--    (탈퇴 유저 여권 삭제 시 다른 유저의 likes/scrap도 연쇄 삭제)
-- =============================================

-- passport_image
ALTER TABLE ONLY public.passport_image
    DROP CONSTRAINT fk2v26bdu2o9b5i8mv3ef8ldyv1;
ALTER TABLE ONLY public.passport_image
    ADD CONSTRAINT fk2v26bdu2o9b5i8mv3ef8ldyv1 FOREIGN KEY (passport_id) REFERENCES public.passport(passport_id) ON DELETE CASCADE;

-- passport_stamp
ALTER TABLE ONLY public.passport_stamp
    DROP CONSTRAINT fks68ubk271qdme3btnhlsf09mo;
ALTER TABLE ONLY public.passport_stamp
    ADD CONSTRAINT fks68ubk271qdme3btnhlsf09mo FOREIGN KEY (passport_id) REFERENCES public.passport(passport_id) ON DELETE CASCADE;

-- likes (다른 유저가 이 여권에 누른 좋아요)
ALTER TABLE ONLY public.likes
    DROP CONSTRAINT fkltqvd5mqg4c0dhmt241qnfuny;
ALTER TABLE ONLY public.likes
    ADD CONSTRAINT fkltqvd5mqg4c0dhmt241qnfuny FOREIGN KEY (passport_id) REFERENCES public.passport(passport_id) ON DELETE CASCADE;

-- scrap (다른 유저가 이 여권을 스크랩)
ALTER TABLE ONLY public.scrap
    DROP CONSTRAINT fklsmigjultu7jx5u3f8diygwv3;
ALTER TABLE ONLY public.scrap
    ADD CONSTRAINT fklsmigjultu7jx5u3f8diygwv3 FOREIGN KEY (passport_id) REFERENCES public.passport(passport_id) ON DELETE CASCADE;

