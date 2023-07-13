package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.ImageSqlQueries
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.security.InvalidParameterException
import javax.imageio.ImageIO


class ImagesSubRouter(vertx: Vertx, pgPool: PgPool, uploadsDir: String, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pgPool, jwtAuth) {

  private val imageSqlQueries = ImageSqlQueries();
  private var uploadsDir = ""

  init {

    router.get("/").handler(this::handleGetImagesForGallery)
    router.post("/").handler(this::handleCreateImage)
    router.post("/update-order").handler(this::handleUpdateOrder)
    router.delete("/:id").handler(this::handleDeleteImage)

    this.uploadsDir = uploadsDir
  }


  /**
   * Get a list of images for gallery
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/rates</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>gallery_id</code> - integer
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *     <li>
   *       <code>rows</code> - array
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>filename</code> - text</li>
   *           <li><code>content_type</code> - text</li>
   *           <li><code>file_size</code> - text</li>
   *           <li><code>order_by</code> - integer</li>
   *           <li><code>url</code> - text</li>
   *           <li><code>thumbnail_url</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleGetImagesForGallery(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("gallery_id").toInt()
        val images = execQuery(imageSqlQueries.getImagesForGallery, Tuple.of(id))

        sendJsonPayload(ctx, json {
          obj(
            "rows" to images.map {
              val jsonObj = it.toJson()

              jsonObj.put("url", getUrl(jsonObj))
              jsonObj.put("thumbnail_url", getThumbnailUrl(jsonObj))

              jsonObj
            }
          )
        })
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }


  /**
   * Create a Rate for a building and season
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/rates</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>gallery_id</code> - integer
   *   </li>
   *   <li>
   *     <code>file</code> - file
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params (Json Format):</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *     <li>
   *       <code>data</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>filename</code> - text</li>
   *           <li><code>content_type</code> - text</li>
   *           <li><code>file_size</code> - text</li>
   *           <li><code>order_by</code> - integer</li>
   *           <li><code>url</code> - text</li>
   *           <li><code>thumbnail_url</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleCreateImage(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        if(ctx.request().getParam("gallery_id").isNullOrBlank()){
          throw InvalidParameterException("Gallery Id is required")
        }
        val galleryId: Int = ctx.request().getParam("gallery_id").toInt()
        val uploads = ctx.fileUploads()

        if(uploads.size == 0){
          throw InvalidParameterException("Must have one file upload")
        }

        val maxOrderBy = execQuery(imageSqlQueries.getMaxOrderByForImagesInGallery, Tuple.of(galleryId)).first().getInteger("max")

        ctx.fileUploads().forEach { fileUpload ->
          val fileName = fileUpload.fileName()
          val size = fileUpload.size()
          val contentType = fileUpload.contentType()

          val images = execQuery(imageSqlQueries.insertImage, Tuple.of(galleryId, maxOrderBy+1, fileName, contentType, size))
          val image = images.first()


          var id = image.getInteger("id")
          var location = "$uploadsDir/$id/${fileName}"

          if(vertx.fileSystem().existsBlocking("$uploadsDir/$id")){
            vertx.fileSystem().deleteRecursiveBlocking("$uploadsDir/$id", true)
          }
          var locationParts = location.split("/")
          var folderStructure = ""
          for (locationPart in locationParts) {
            if(locationPart != fileName) {
              folderStructure += locationPart + "/";

              if(!vertx.fileSystem().existsBlocking(folderStructure)){
                vertx.fileSystem().mkdirBlocking(folderStructure);
              }
            }
          }
          vertx.fileSystem().copyBlocking(fileUpload.uploadedFileName(), location)

          val maxw = 200
          val maxh = 200
          val uploadedFile: File = File(fileUpload.uploadedFileName())
          val img = ImageIO.read(uploadedFile)
          val w = img.getWidth(null)
          val h = img.getHeight(null)
          var scaledw = w
          var scaledh = h

          if (w > maxw) {
            //scale width to fit
            scaledw = maxw
            scaledh = maxw * h / w
          }

          if (h > maxh) {
            scaledh = maxh
            scaledw = maxh * w / h
          }
          val scaledImg = img.getScaledInstance(scaledw, scaledh, Image.SCALE_SMOOTH)

          val img2 = BufferedImage(
            scaledw,
            scaledh,
            if (img.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
          )

          val g = img2.graphics
          g.drawImage(scaledImg, 0, 0, null)
          g.dispose()

          val thumbFile = "${fileName.substringBeforeLast(".")}_thumb.png"

          var thumbLocation = "$uploadsDir/$id/${thumbFile}"
          val outputfile = File(thumbLocation)
          ImageIO.write(img2, "png", outputfile)


        val jsonObj = image.toJson()
        jsonObj.put("url", getUrl(jsonObj))
        jsonObj.put("thumbnail_url", getThumbnailUrl(jsonObj))


          sendJsonPayload(ctx, json {
            obj(
            "data" to jsonObj
            )
          })
        }

      } catch (e: InvalidParameterException) {
        logger.error(e.printStackTrace())
        fail400(ctx, e)
      } catch (e: IllegalArgumentException) {
        logger.error(e.printStackTrace())
        fail400(ctx, e)
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }


  /**
   * Update a Order by for image
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/images/update-order</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params (Json Format):</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>image_ids</code> - integer array
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleUpdateOrder(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()

        val imageIds = data.getJsonArray("image_ids")

        imageIds.forEachIndexed { index, it ->
          val imageId = it as Int

          execQuery(imageSqlQueries.updateImageOrderBy, Tuple.of(index+1, imageId))
        }

        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: InvalidParameterException) {
        logger.error(e.printStackTrace())
        fail400(ctx, e)
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }


  /**
   * Delete an image
   *
   * <p>
   *   <b>Method:</b> <code>DELETE</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/images/:id</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>id</code> - integer
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params (Json Format):</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleDeleteImage(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("id").toInt()

        val deletedImage = execQuery(imageSqlQueries.deleteImage, Tuple.of(id)).first()

        vertx.fileSystem().deleteRecursiveBlocking("$uploadsDir/$id", true)

        val remainingImages = execQuery(imageSqlQueries.getImagesForGallery, Tuple.of(deletedImage.getInteger("id")))

        remainingImages.forEachIndexed { index, row ->
          execQuery(imageSqlQueries.updateImageOrderBy, Tuple.of(index+1, row.getInteger("id")))
        }


        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }


  private fun getUrl(obj: JsonObject): String {
    return "/gallery-images/${obj.getInteger("id")}/${obj.getString("filename")}"
  }

  private fun getThumbnailUrl(obj: JsonObject): String {
    val filename = obj.getString("filename").substringBeforeLast(".")

    return "/gallery-images/${obj.getInteger("id")}/${filename}_thumb.png"
  }
}
