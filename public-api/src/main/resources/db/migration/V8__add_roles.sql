create table roles
(
  id SERIAL PRIMARY KEY,
  name text
);

insert into roles (name) values ('Administrator'), ('User');

alter table users add column role_id integer;

ALTER TABLE users
  ADD CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles (id);
