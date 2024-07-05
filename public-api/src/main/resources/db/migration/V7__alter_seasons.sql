alter table seasons rename column pre_season_start_date to high_season_start_date;
alter table seasons rename column post_season_end_date to high_season_end_date;

-- update existing entry
update seasons set start_date = '2023-06-01', high_season_start_date = '2023-06-10', high_season_end_date = '2023-09-15', end_date = '2023-10-08' where  name = '2023';
