alter table buildings add column identifier text unique;

update buildings set identifier = 'all' where name = 'All Buildings';
update buildings set identifier = 'lodge' where name = 'The Lodge';
update buildings set identifier = 'lakeside' where name = 'Lakeside Cabin';
update buildings set identifier = 'treehouse' where name = 'Treehouse Cabin';
update buildings set identifier = 'birdsnest' where name = 'Bird''s Nest Cabin';
update buildings set identifier = 'judys' where name = 'Judy''s Cabin';
