insert into seasons (name, pre_season_start_date, start_date, end_date, post_season_end_date) values
  ('2023', '2023-06-01', '2023-06-10', '2023-09-15', '2023-10-08'),
  ('2024', '2024-06-01', '2024-06-01', '2024-09-14', '2024-10-05');





insert into rates (season_id, building_id, high_season_rate, low_season_rate) values
  ((select id from seasons where name = '2023'), 1, 8100, 7595),
  ((select id from seasons where name = '2023'), 2, 4320, 4100),
  ((select id from seasons where name = '2023'), 3, 1750, 1560),
  ((select id from seasons where name = '2023'), 4, 1160, 1100),
  ((select id from seasons where name = '2023'), 5, 1128, 1075),
  ((select id from seasons where name = '2023'), 6, 1680, 1530),
  ((select id from seasons where name = '2024'), 1, 8700, 7695),
  ((select id from seasons where name = '2024'), 2, 4586, 4320),
  ((select id from seasons where name = '2024'), 3, 1858, 1620),
  ((select id from seasons where name = '2024'), 4, 1160, 1100),
  ((select id from seasons where name = '2024'), 5, 1232, 1075),
  ((select id from seasons where name = '2024'), 6, 1783, 1530);

