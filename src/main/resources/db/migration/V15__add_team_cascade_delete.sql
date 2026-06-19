-- 팀 해산 시 district_cover 팀 커버 FK 위반 수정 (Issue #177 관련)
-- district_cover.team_id → team.team_id: ON DELETE CASCADE 추가

ALTER TABLE ONLY public.district_cover
    DROP CONSTRAINT fk_district_cover_team;
ALTER TABLE ONLY public.district_cover
    ADD CONSTRAINT fk_district_cover_team FOREIGN KEY (team_id) REFERENCES public.team(team_id) ON DELETE CASCADE;
