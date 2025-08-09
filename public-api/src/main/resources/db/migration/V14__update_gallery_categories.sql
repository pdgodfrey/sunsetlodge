alter table gallery_categories alter column description set default '';

update gallery_categories set description = '' where description is null;

