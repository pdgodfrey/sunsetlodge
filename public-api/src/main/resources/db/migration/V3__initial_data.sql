alter table seasons add column pre_season_start_date date;
alter table seasons add column post_season_end_date date;

alter table buildings add column max_occupancy int;
alter table buildings add column bedrooms int;
alter table buildings add column bathrooms int;


create table rates
(
  id SERIAL PRIMARY KEY,
  season_id int,
  building_id int,
  high_season_rate int,
  low_season_rate int,
  CONSTRAINT fk_season
    FOREIGN KEY(season_id)
      REFERENCES seasons(id),
  CONSTRAINT fk_building
    FOREIGN KEY(building_id)
      REFERENCES buildings(id)
);


insert into buildings (name, max_occupancy, bedrooms, bathrooms) values
                                                                   ('All Buildings', 26, 13, 9),
                                                                   ('The Lodge', 10, 5, 4),
                                                                   ('Lakeside Cabin', 6, 3, 2),
                                                                   ('Treehouse Cabin', 2, 1, 1),
                                                                   ('Bird''s Nest Cabin', 2, 1, 1),
                                                                   ('Judy''s Nest Cabin', 6, 1, 3);



