package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.RateSqlQueries
import java.security.InvalidParameterException


class RatesSubRouter(vertx: Vertx, pgPool: PgPool) : BaseSubRouter(vertx, pgPool) {

  private val rateSqlQueries = RateSqlQueries();

  init {

    router.get("/").handler(this::handleGetRatesForSeason)
    router.post("/").handler(this::handleCreateRate)
    router.put("/:id").handler(this::handleUpdateRate)
    router.delete("/:id").handler(this::handleDeleteRate)
  }


  /**
   * Get a list of rates for a given season
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
   *     <code>season_id</code> - integer
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
   *           <li><code>season_id</code> - integer</li>
   *           <li><code>building_id</code> - integer</li>
   *           <li><code>high_season_rate</code> - integer</li>
   *           <li><code>low_season_rate</code> - integer</li>
   *           <li><code>season_name</code> - text</li>
   *           <li><code>building_name</code> - text</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleGetRatesForSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("season_id").toInt()
        val rates = execQuery(rateSqlQueries.getRatesForSeason, Tuple.of(id))

        sendJsonPayload(ctx, json {
          obj(
            "rows" to rates.map { it.toJson() }
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
   *     <code>season_id</code> - integer
   *   </li>
   *   <li>
   *     <code>building_id</code> - integer
   *   </li>
   *   <li>
   *     <code>high_season_rate</code> - integer
   *   </li>
   *   <li>
   *     <code>low_season_rate</code> - integer
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
   *           <li><code>season_id</code> - integer</li>
   *           <li><code>building_id</code> - integer</li>
   *           <li><code>high_season_rate</code> - integer</li>
   *           <li><code>low_season_rate</code> - integer</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleCreateRate(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()

        validateRateData(data)

        checkForExistingRate(data)

        val params = Tuple.tuple()
        params.addInteger(data.getInteger("season_id"))
        params.addInteger(data.getInteger("building_id"))
        params.addInteger(data.getInteger("high_season_rate"))
        params.addInteger(data.getInteger("low_season_rate"))

        val rate = execQuery(rateSqlQueries.createRate, params)

        sendJsonPayload(ctx, json {
          obj(
            "data" to rate.first().toJson()
          )
        })
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
   * Update a Rate for a building and season
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/rates/:id</code>
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
   *     <code>season_id</code> - integer
   *   </li>
   *   <li>
   *     <code>building_id</code> - integer
   *   </li>
   *   <li>
   *     <code>high_season_rate</code> - integer
   *   </li>
   *   <li>
   *     <code>low_season_rate</code> - integer
   *   </li>
   *   <li>
   *     <code>id</code> - integer
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
   *           <li><code>season_id</code> - integer</li>
   *           <li><code>building_id</code> - integer</li>
   *           <li><code>high_season_rate</code> - integer</li>
   *           <li><code>low_season_rate</code> - integer</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleUpdateRate(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()

        validateRateData(data)

        val id = ctx.request().getParam("id").toInt()

        val params = Tuple.tuple()
        params.addInteger(data.getInteger("high_season_rate"))
        params.addInteger(data.getInteger("low_season_rate"))
        params.addInteger(id)

        val rate = execQuery(rateSqlQueries.updateRate, params)

        sendJsonPayload(ctx, json {
          obj(
            "data" to rate.first().toJson()
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
   * Delete a Rate for a building and season
   *
   * <p>
   *   <b>Method:</b> <code>DELETE</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/rates/:id</code>
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
  fun handleDeleteRate(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("id").toInt()

        execQuery(rateSqlQueries.deleteRate, Tuple.of(id))

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

  fun validateRateData(data: JsonObject?){
    if (data == null) {
      throw InvalidParameterException("No request body")
    } else {
      val invalidFields = ArrayList<String>()
      if (!data.containsKey("season_id") || data.getInteger("season_id") == null) {
        invalidFields.add("Season is a required field")
      }
      if (!data.containsKey("building_id") || data.getInteger("building_id") == null) {
        invalidFields.add("Building is a required field")
      }

      if(invalidFields.size > 0){
        throw InvalidParameterException(invalidFields.joinToString(", "))
      }
    }
  }

  suspend fun checkForExistingRate(data: JsonObject){
    val params = Tuple.tuple()
    params.addInteger(data.getInteger("season_id"))
    params.addInteger(data.getInteger("building_id"))

    val existingRate = execQuery(rateSqlQueries.getRateForSeasonAndBuilding, params)

    if(existingRate.size() > 0){
      throw IllegalArgumentException("Rate already exists for this building and season")
    }
  }
}
