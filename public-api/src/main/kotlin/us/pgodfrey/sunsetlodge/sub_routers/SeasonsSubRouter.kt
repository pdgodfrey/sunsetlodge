package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.MiscSqlQueries
import us.pgodfrey.sunsetlodge.sql.RateSqlQueries
import us.pgodfrey.sunsetlodge.sql.SeasonSqlQueries
import java.security.InvalidParameterException
import java.time.LocalDate


class SeasonsSubRouter(vertx: Vertx, pool: Pool, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pool, jwtAuth) {

  private val seasonSqlQueries = SeasonSqlQueries()
  private val rateSqlQueries = RateSqlQueries()
  private val miscSqlQueries = MiscSqlQueries()

  init {

    router.get("/current").coroutineHandler(this::handleGetCurrentSeason)

    router.get("/").coroutineHandler(this::handleGetSeasons)
    router.get("/:id").coroutineHandler(this::handleGetSeason)
    router.post("/").coroutineHandler(this::handleCreateSeason)
    router.put("/:id").coroutineHandler(this::handleUpdateSeason)
    router.delete("/:id").coroutineHandler(this::handleDeleteSeason)
  }

  /**
   * Get the "current" season
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/seasons/current</code>
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
   *       <code>season</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>end_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>is_open</code> - boolean</li>
   *           <li><code>high_season_start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>high_season_end_date</code> - text (yyyy-mm-dd)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleGetCurrentSeason(ctx: RoutingContext) {
    try {
      val seasons = execQuery(seasonSqlQueries.getCurrentSeason)

      sendJsonPayload(ctx, json {
        obj(
          "season" to seasons.map { it.toJson() }.first()
        )
      })
    } catch (e: Exception) {
      e.printStackTrace()
      fail500(ctx, e)
    }
  }

  /**
   * Get the seasons
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/seasons</code>
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
   *           <li><code>name</code> - text</li>
   *           <li><code>start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>end_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>is_open</code> - boolean</li>
   *           <li><code>high_season_start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>high_season_end_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>is_current</code> - boolean</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleGetSeasons(ctx: RoutingContext) {
    try {
      val seasons = execQuery(seasonSqlQueries.getSeasons)

      sendJsonPayload(ctx, json {
        obj(
          "rows" to seasons.map { it.toJson() }
        )
      })
    } catch (e: Exception) {
      e.printStackTrace()
      fail500(ctx, e)
    }
  }


  /**
   * Get an individual season
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/seasons/:id</code>
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
   *       <code>season</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>end_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>is_open</code> - boolean</li>
   *           <li><code>high_season_start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>high_season_end_date</code> - text (yyyy-mm-dd)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleGetSeason(ctx: RoutingContext) {
    try {
      val id = ctx.request().getParam("id").toInt()
      val seasons = execQuery(seasonSqlQueries.getSeason, Tuple.of(id))

      sendJsonPayload(ctx, json {
        obj(
          "season" to seasons.map { it.toJson() }.first()
        )
      })
    } catch (e: Exception) {
      e.printStackTrace()
      fail500(ctx, e)
    }
  }


  /**
   * Create a Season
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/seasons</code>
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
   *     <code>name</code> - text
   *   </li>
   *   <li>
   *     <code>start_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>end_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>high_season_start_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>high_season_end_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>is_open</code> - boolean
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
   *           <li><code>start_date</code> - text</li>
   *           <li><code>end_date</code> - text</li>
   *           <li><code>is_open</code> - boolean</li>
   *           <li><code>high_season_start_date</code> - text</li>
   *           <li><code>high_season_end_date</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleCreateSeason(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()

      validateSeasonData(data)

      val params = Tuple.tuple()
      params.addString(data.getString("name"))
      params.addLocalDate(LocalDate.parse(data.getString("start_date")))
      params.addLocalDate(LocalDate.parse(data.getString("end_date")))
      params.addLocalDate(LocalDate.parse(data.getString("high_season_start_date")))
      params.addLocalDate(LocalDate.parse(data.getString("high_season_end_date")))
      params.addBoolean(data.getBoolean("is_open"))
      params.addInteger(data.getInteger("sheet_rate"))
      params.addInteger(data.getInteger("boat_package_rate"))
      params.addInteger(data.getInteger("boat_separate_rate"))

      val seasonResult = execQuery(seasonSqlQueries.createSeason, params)
      val season = seasonResult.first()

      val buildings = execQuery(miscSqlQueries.getBuildings)
      buildings.forEach { building ->
        val rateParams = Tuple.of(
          season.getInteger("id"),
          building.getInteger("id"),
          null,
          null,
          null,
          null
        )
        execQuery(rateSqlQueries.createRate, rateParams)
      }


      sendJsonPayload(ctx, json {
        obj(
          "data" to season.toJson()
        )
      })
    } catch (e: InvalidParameterException) {
      e.printStackTrace()
      fail400(ctx, e)
    } catch (e: Exception) {
      e.printStackTrace()
      fail500(ctx, e)
    }
  }

  /**
   * Update a Season
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/seasons/:id</code>
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
   *     <code>name</code> - text
   *   </li>
   *   <li>
   *     <code>start_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>end_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>high_season_start_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>high_season_end_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>is_open</code> - boolean
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
   *           <li><code>start_date</code> - text</li>
   *           <li><code>end_date</code> - text</li>
   *           <li><code>is_open</code> - boolean</li>
   *           <li><code>high_season_start_date</code> - text</li>
   *           <li><code>high_season_end_date</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleUpdateSeason(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()

      logger.info("=================")
      logger.info(data.encodePrettily())

      validateSeasonData(data)

      val id = ctx.request().getParam("id").toInt()

      val params = Tuple.tuple()
      params.addString(data.getString("name"))
      params.addLocalDate(LocalDate.parse(data.getString("start_date")))
      params.addLocalDate(LocalDate.parse(data.getString("end_date")))
      params.addLocalDate(LocalDate.parse(data.getString("high_season_start_date")))
      params.addLocalDate(LocalDate.parse(data.getString("high_season_end_date")))
      params.addBoolean(data.getBoolean("is_open"))
      params.addInteger(data.getInteger("sheet_rate"))
      params.addInteger(data.getInteger("boat_package_rate"))
      params.addInteger(data.getInteger("boat_separate_rate"))
      params.addInteger(id)

      val season = execQuery(seasonSqlQueries.updateSeason, params)

      sendJsonPayload(ctx, json {
        obj(
          "data" to season.first().toJson()
        )
      })
    } catch (e: InvalidParameterException) {
      e.printStackTrace()
      fail400(ctx, e)
    } catch (e: Exception) {
      e.printStackTrace()
      fail500(ctx, e)
    }
  }

  /**
   * Delete a Sesaon
   *
   * <p>
   *   <b>Method:</b> <code>DELETE</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/seasons/:id</code>
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
  private suspend fun handleDeleteSeason(ctx: RoutingContext) {
    try {
      val id = ctx.request().getParam("id").toInt()

      execQuery(rateSqlQueries.deleteRatesForSeason, Tuple.of(id))

      execQuery(seasonSqlQueries.deleteSeason, Tuple.of(id))

      sendJsonPayload(ctx, json {
        obj(

        )
      })
    } catch (e: Exception) {
      e.printStackTrace()
      fail500(ctx, e)
    }
  }

  private fun validateSeasonData(data: JsonObject?){
    if (data == null) {
      throw InvalidParameterException("No request body")
    } else {
      val invalidFields = ArrayList<String>()
      if (!data.containsKey("name") || data.getString("name").isNullOrBlank()) {
        invalidFields.add("Name is a required field")
      }
      if (!data.containsKey("start_date") || data.getString("start_date").isNullOrBlank()) {
        invalidFields.add("Start Date is a required field")
      }
      if (!data.containsKey("end_date") || data.getString("end_date").isNullOrBlank()) {
        invalidFields.add("End Date is a required field")
      }
      if (!data.containsKey("high_season_start_date") || data.getString("high_season_start_date").isNullOrBlank()) {
        invalidFields.add("High Season Start Date is a required field")
      }
      if (!data.containsKey("high_season_end_date") || data.getString("high_season_end_date").isNullOrBlank()) {
        invalidFields.add("High Season End Date is a required field")
      }
      if (!data.containsKey("is_open") || !(data.getValue("is_open") is Boolean)) {
        invalidFields.add("Is Open is a required field")
      }

      if(invalidFields.size > 0){
        throw InvalidParameterException(invalidFields.joinToString(", "))
      }
    }
  }
}
