create table users
(
  id SERIAL PRIMARY KEY,
  name text,
  email  text NOT NULL,
  password text NOT NULL,
  reset_token timestamp,
  created_at timestamp,
  updated_at timestamp
);

create table seasons
(
  id   SERIAL PRIMARY KEY,
  name text,
  start_date date,
  end_date date,
  is_open boolean default false
);

create table buildings
(
  id   SERIAL PRIMARY KEY,
  name text
);

create table areas
(
  id   SERIAL PRIMARY KEY,
  building_id int,
  name text,
  description text,
  is_bookable boolean default false,
  order_by int not null,
  CONSTRAINT fk_building
    FOREIGN KEY(building_id)
      REFERENCES buildings(id)
);

create table bookings
(
  id SERIAL PRIMARY KEY,
  season_id int,
  name text,
  start_date date,
  end_date date,
  CONSTRAINT fk_season
    FOREIGN KEY(season_id)
      REFERENCES seasons(id)
);

create table bookings_buildings
(
  booking_id int,
  building_id int,
  CONSTRAINT fk_booking
    FOREIGN KEY(booking_id)
      REFERENCES bookings(id),
  CONSTRAINT fk_building
    FOREIGN KEY(building_id)
      REFERENCES buildings(id)
);

create table galleries
(
  id serial primary key,
  identifier text
);

create table images
(
  id serial primary key
)
