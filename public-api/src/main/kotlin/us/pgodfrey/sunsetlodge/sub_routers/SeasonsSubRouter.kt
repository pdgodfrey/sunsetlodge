package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.SeasonSqlQueries
import java.security.InvalidParameterException
import java.time.LocalDate


class SeasonsSubRouter(vertx: Vertx, pgPool: PgPool) : BaseSubRouter(vertx, pgPool) {

  private val seasonSqlQueries = SeasonSqlQueries();

  init {

    router.get("/current").handler(this::handleGetCurrentSeason)

    router.get("/").handler(this::handleGetSeasons)
    router.get("/:id").handler(this::handleGetSeason)
    router.post("/").handler(this::handleCreateSeason)
    router.put("/:id").handler(this::handleUpdateSeason)
    router.delete("/:id").handler(this::handleDeleteSeason)
  }

  fun handleGetCurrentSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
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
  }

  fun handleGetSeasons(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
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
  }

  fun handleGetSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
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
  }

  fun handleCreateSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
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

        val season = execQuery(seasonSqlQueries.createSeason, params)

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
  }

  fun handleUpdateSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()

        validateSeasonData(data)

        val id = ctx.request().getParam("id").toInt()

        val params = Tuple.tuple()
        params.addString(data.getString("name"))
        params.addLocalDate(LocalDate.parse(data.getString("start_date")))
        params.addLocalDate(LocalDate.parse(data.getString("end_date")))
        params.addLocalDate(LocalDate.parse(data.getString("high_season_start_date")))
        params.addLocalDate(LocalDate.parse(data.getString("high_season_end_date")))
        params.addBoolean(data.getBoolean("is_open"))
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
  }

  fun handleDeleteSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("id").toInt()

        val season = execQuery(seasonSqlQueries.deleteSeason, Tuple.of(id))

        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: Exception) {
        e.printStackTrace()
        fail500(ctx, e)
      }
    }
  }

  fun validateSeasonData(data: JsonObject?){
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
