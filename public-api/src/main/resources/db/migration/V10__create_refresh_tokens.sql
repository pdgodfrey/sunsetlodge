
create table refresh_tokens
(
  refresh_token text PRIMARY KEY,
  user_id integer,
  refresh_token_expiration timestamp,
  is_used boolean default false
);
