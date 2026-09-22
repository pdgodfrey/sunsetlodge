alter table seasons add column bookings_open_date date;


update seasons set bookings_open_date = '2025-09-15' where name = '2026';
update seasons set bookings_open_date = '2026-09-15' where name = '2027';

alter table seasons drop column is_open;

alter table seasons add column is_closed boolean default false;
update seasons set is_closed = true where name = '2026';
