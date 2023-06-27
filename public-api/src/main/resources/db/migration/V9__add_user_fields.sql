alter table users rename column reset_token to reset_token_expiration;

alter table users add column reset_token text;
