package us.pgodfrey.sunsetlodge.sql

data class ImageSqlQueries (

  val getImagesForGallery: String = "select * from images where gallery_id = $1 order by order_by",

  val insertImage: String = "insert into images (gallery_id, order_by, filename, content_type, file_size) values " +
    "($1, $2, $3, $4, $5) returning *",

  val updateImageOrderBy: String = "update images set order_by = $1 where id = $2",

  val deleteImage: String = "delete from images where id = $1 returning *",

  val getMaxOrderByForImagesInGallery: String = "select coalesce(max(order_by), 0) as max from images where gallery_id = $1",

  val getRandomImageForGallery: String = "select * from images where gallery_id = $1 order by random() limit 1"

)
