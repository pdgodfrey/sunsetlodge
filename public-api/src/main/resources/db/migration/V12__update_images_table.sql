alter table images add column gallery_id integer;

ALTER TABLE images
  ADD CONSTRAINT fk_image_gallery FOREIGN KEY (gallery_id) REFERENCES galleries (id);

alter table images add column order_by integer;

alter table images add column filename text;
alter table images add column content_type text;
alter table images add column file_size integer;

