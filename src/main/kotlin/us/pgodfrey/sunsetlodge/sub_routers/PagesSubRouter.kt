package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.PageSqlQueries


class PagesSubRouter(vertx: Vertx, pgPool: PgPool) : BaseSubRouter(vertx, pgPool) {


  private val sqlQueries = PageSqlQueries();
  private lateinit var engine: HandlebarsTemplateEngine

  init {

    engine = HandlebarsTemplateEngine.create(vertx)

    router.route("/*").handler { ctx: RoutingContext ->

      logger.info(
        "=====" + ctx.request().method() + ": " + ctx.normalizedPath() + " : " + ctx.request().absoluteURI()
      )

      ctx.next()
    }
    router.get("/").handler(this::handleHome)
    router.get("/hello-world").handler(this::handleHelloWorld)
    router.get("/rates-and-availability").handler(this::handleRatesAndAvailability)
    router.get("/contact-us").handler(this::handleContactUs)
    router.get("/things-to-do").handler(this::handleThingstoDo)
    router.get("/lodges-and-cabins").handler(this::handleLodgesandCabins)
  }

  fun handleHome(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data: JsonObject = JsonObject()
          .put("title", "Seasons of the year")

        val seasons = JsonArray()
        seasons.add(JsonObject().put("name", "Spring"))
        seasons.add(JsonObject().put("name", "Summer"))
        seasons.add(JsonObject().put("name", "Autumn"))
        seasons.add(JsonObject().put("name", "Winter"))

        data.put("seasons", seasons)

        engine.render(data, "pages/home.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
  fun handleHelloWorld(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data: JsonObject = JsonObject()
          .put("title", "Hello World")

        engine.render(data, "pages/hello-world.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun handleRatesAndAvailability(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data: JsonObject = JsonObject()

        data.put("title", "Rates and Availability")

        val currentSeason = execQuery(sqlQueries.currentSeason).first()
        data.put("current_season", currentSeason.toJson())

        var highSeasonRates = execQuery(sqlQueries.getHighSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
          .map {
            it.toJson()
          }

        data.put("high_current_rates", highSeasonRates)

        var lowSeasonRates = execQuery(sqlQueries.getLowSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
          .map {
            it.toJson()
          }

        data.put("low_current_rates", lowSeasonRates)

        engine.render(data, "pages/rates-and-availability.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
  fun handleContactUs(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data: JsonObject = JsonObject()
          .put("title", "Contact Us")

        engine.render(data, "pages/contact-us.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun handleThingstoDo(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data: JsonObject = JsonObject()
          .put("title", "Things to Do")

        engine.render(data, "pages/things-to-do.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
  fun handleLodgesandCabins(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data: JsonObject = JsonObject()
          .put("title", "Lodges and Cabins")

        engine.render(data, "pages/lodges-and-cabins.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
