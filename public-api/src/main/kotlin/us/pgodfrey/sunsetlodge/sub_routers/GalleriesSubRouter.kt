package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.GallerySqlQueries
import java.security.InvalidParameterException


class GalleriesSubRouter(vertx: Vertx, pool: Pool, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pool, jwtAuth) {


  private val galleriesSqlQueries = GallerySqlQueries()

  init {
    router.get("/gallery-categories").coroutineHandler(this::handleGetGalleryCategories)
    router.put("/gallery-categories/:id").coroutineHandler(this::handleUpdateGalleryCategory)

    router.get("/").coroutineHandler(this::handleGetGalleries)
    router.put("/:id").coroutineHandler(this::handleUpdateGallery)
  }



  /**
   * Get a list of Gallery Categories
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/galleries</code>
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
   *           <li><code>identifier</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleGetGalleryCategories(ctx: RoutingContext) {
    try {
      val galleryCategories = execQuery(galleriesSqlQueries.getGalleryCategories)

      sendJsonPayload(ctx, json {
        obj(
          "rows" to galleryCategories.map { it.toJson() }
        )
      })
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Update a Gallery Category Description
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/galleries/gallery-categories/:id</code>
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
   *     <code>id</code> - integer
   *   </li>
   *   <li>
   *     <code>description</code> - text
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
   *       <code>data</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>description</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleUpdateGalleryCategory(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()


      val id = ctx.request().getParam("id").toInt()

      val params = Tuple.tuple()
      params.addString(data.getString("description"))
      params.addInteger(id)

      val updatedCategory = execQuery(galleriesSqlQueries.updateGalleryCategoryDescription, params)

      sendJsonPayload(ctx, json {
        obj(
          "data" to updatedCategory.first().toJson()
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


  /**
   * Get a list of Galleries
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/galleries</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>gallery_category_id</code> - integer
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
   *           <li><code>identifier</code> - text</li>
   *           <li><code>gallery_category_id</code> - integer</li>
   *           <li><code>order_by</code> - integer</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleGetGalleries(ctx: RoutingContext) {
    try {
      val galleryCategoryId = ctx.request().getParam("gallery_category_id").toInt()

      val galleries = execQuery(galleriesSqlQueries.getGalleriesForCategory, Tuple.of(galleryCategoryId))

      sendJsonPayload(ctx, json {
        obj(
          "rows" to galleries.map { it.toJson() }
        )
      })
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }


  /**
   * Update a Gallery Description
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/galleries/:id</code>
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
   *     <code>id</code> - integer
   *   </li>
   *   <li>
   *     <code>description</code> - text
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
   *       <code>data</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>description</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleUpdateGallery(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()


      val id = ctx.request().getParam("id").toInt()

      val params = Tuple.tuple()
      params.addString(data.getString("description"))
      params.addInteger(id)

      val updatedGallery = execQuery(galleriesSqlQueries.updateGalleryDescription, params)

      sendJsonPayload(ctx, json {
        obj(
          "data" to updatedGallery.first().toJson()
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
