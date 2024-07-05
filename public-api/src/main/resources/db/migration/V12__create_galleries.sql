drop table areas;

create table gallery_categories
(
  id serial primary key,
  name text,
  description text
);

insert into gallery_categories (name) values ('The Lodge And Cabins'), ('Sunsets'), ('Weddings'), ('Backgrounds');

alter table galleries add column gallery_category_id integer;

ALTER TABLE galleries
  ADD CONSTRAINT fk_gallery_gallery_category FOREIGN KEY (gallery_category_id) REFERENCES gallery_categories (id);

alter table galleries add column order_by integer;

alter table galleries add column description text;

insert into galleries (identifier, gallery_category_id, order_by) values
          ('The Lodge', 1, 1),
          ('Lakeside Cabin', 1, 2),
          ('Judy''s Cabin', 1, 3),
          ('Tree House Cabin', 1, 4),
          ('Bird''s Nest Cabin', 1, 5),
          ('The Grounds', 1, 6),
          ('Sunsets', 2, 1),
          ('Godfrey/Kahn', 3, 1),
          ('Powers/Fuqua', 3, 2),
          ('Backgrounds', 4, 1);
