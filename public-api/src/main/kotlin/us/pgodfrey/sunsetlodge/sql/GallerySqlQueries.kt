package us.pgodfrey.sunsetlodge.sql

data class GallerySqlQueries (

  val getGalleryCategories: String = "select * from gallery_categories order by id",
  val updateGalleryCategoryDescription: String = "update gallery_categories set description = $1 where id = $2 returning *",

  val getGalleriesForCategory: String = "select galleries.*, (select count(*) from images where gallery_id = galleries.id) as num_images " +
    "from galleries where gallery_category_id = $1 order by order_by",
  val updateGalleryDescription: String = "update galleries set description = $1 where id = $2 returning *",

  val getGalleryCategoryByName: String = "select * from gallery_categories where name = $1",
  val getGalleryByIdentifierAndCategoryId: String = "select * from galleries where identifier = $1 and gallery_category_id = $2",

)
