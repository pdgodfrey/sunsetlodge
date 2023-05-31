insert into seasons (name, pre_season_start_date, start_date, end_date, post_season_end_date) values
  ('2023', '2023-06-01', '2023-06-10', '2023-09-15', '2023-10-08');





insert into rates (season_id, building_id, high_season_rate, low_season_rate) values
                                                                                ((select id from seasons where name = '2023'), 1, 8100, 7595),
                                                                                ((select id from seasons where name = '2023'), 2, 4320, 4100),
                                                                                ((select id from seasons where name = '2023'), 3, 1750, 1560),
                                                                                ((select id from seasons where name = '2023'), 4, 1160, 1100),
                                                                                ((select id from seasons where name = '2023'), 5, 1128, 1075),
                                                                                ((select id from seasons where name = '2023'), 6, 1680, 1530);

