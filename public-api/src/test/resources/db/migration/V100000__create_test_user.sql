insert into users (name, email, role_id, reset_token, password, reset_token_expiration, created_at, updated_at) values
       ('Test Admin', 'test@admin.com', 1, '123', '', now() + interval '10 minutes', now(), now());
