package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonArray
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
import us.pgodfrey.sunsetlodge.sql.MiscSqlQueries
import us.pgodfrey.sunsetlodge.sql.PageSqlQueries
import us.pgodfrey.sunsetlodge.sql.SeasonSqlQueries


class MiscSubRouter(vertx: Vertx, pgPool: PgPool) : BaseSubRouter(vertx, pgPool) {


  private val miscSqlQueries = MiscSqlQueries();
  private val seasonSqlQueries = SeasonSqlQueries();

  init {
    router.get("/buildings").handler(this::handleGetBuildings)
  }

  fun handleGetBuildings(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val buildings = execQuery(miscSqlQueries.getBuildings)

        sendJsonPayload(ctx, json {
          obj(
            "rows" to buildings.map { it.toJson() }
          )
        })
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

}
