
create table news
(
  id SERIAL PRIMARY KEY,
  body text,
  is_published boolean,
  created_at timestamp,
  updated_at timestamp,
  CONSTRAINT published_true_or_null CHECK (is_published),
  CONSTRAINT published_only_1_true UNIQUE (is_published)
);


insert into news (body, is_published, created_at, updated_at) values
      ('Our 2024 season is now available for booking! Check our rates and availability page for details', true, now(), now());


insert into news (body, is_published, created_at, updated_at) values
  ('This is an old piece of news that we should ignore because it''s not published', null, now(), now());
