INSERT INTO member(real_name,game_name,tag_line,solo_tier,solo_rank,solo_lp,balance_score) VALUES
('준하','LunaMaster','KR1','GOLD','II',42,1262),('민수','JungleKing','KR1','PLATINUM','IV',20,1320),
('철수','TopOrFeed','KR1','SILVER','I',80,1185),('영희','MidDiff','KR1','EMERALD','IV',11,1461),
('서현','SupportAngel','KR1','GOLD','IV',55,1205),('동현','ADCOnly','KR1','PLATINUM','III',33,1368),
('지훈','WardMaster','KR1','SILVER','II',44,1114),('상민','SplitPush','KR1','GOLD','I',18,1273),
('현우','SmiteFight','KR1','BRONZE','I',75,1030),('재민','FillPlease','KR1','DIAMOND','IV',5,1605);
INSERT INTO member_position(member_id,position_code,priority_no)
SELECT member_id,p,1 FROM member JOIN (VALUES
('준하','MID'),('민수','JUNGLE'),('철수','TOP'),('영희','MID'),('서현','SUPPORT'),
('동현','ADC'),('지훈','SUPPORT'),('상민','TOP'),('현우','JUNGLE'),('재민','FILL')
) v(n,p) ON real_name=n;
