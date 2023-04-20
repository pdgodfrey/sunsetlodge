package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter


class PagesSubRouter(vertx: Vertx, pgPool: PgPool) : BaseSubRouter(vertx, pgPool) {


  private lateinit var engine: HandlebarsTemplateEngine

  init {

    engine = HandlebarsTemplateEngine.create(vertx)

    router.get("/").handler(this::handleHome)

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

}
