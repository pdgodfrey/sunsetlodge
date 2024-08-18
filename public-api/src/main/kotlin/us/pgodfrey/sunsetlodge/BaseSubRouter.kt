package us.pgodfrey.sunsetlodge

import io.vertx.core.Vertx
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.*


open class BaseSubRouter(val vertx: Vertx, val pool: Pool, jwtAuth: JWTAuth) {
  val logger = LoggerFactory.getLogger(javaClass)

  val router: Router = Router.router(vertx)

  var displaySqlErrors = false

  val scope = CoroutineScope(vertx.dispatcher().asExecutor().asCoroutineDispatcher())

  init {
    val env = System.getenv()

    this.displaySqlErrors = env.getOrDefault("DISPLAY_SQL_ERRORS", "false").toBoolean()

  }


  open fun getSubRouter(): Router {
    return this.router
  }


  fun readinessCheck(ctx: RoutingContext) {
    scope.launch {
      try {
        execQuery("select 1")
        sendJsonPayload(ctx, json { obj( "status" to "UP")})
      } catch (e: Exception) {
        logger.error(e.message)
        sendJsonPayload(ctx, json { obj( "status" to "DOWN")})
      }
    }
  }

  fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit): Route {
    return handler {
      scope.launch {
        try {
          fn(it)
        } catch (e: Exception) {
          it.fail(e)
        }
      }
    }
  }


  suspend fun execQuery(queryStr: String, args: Tuple? = null): RowSet<Row> {
    try {
      return if (args != null) {
        pool.preparedQuery(queryStr).execute(args).coAwait()
      } else {
        pool.preparedQuery(queryStr).execute().coAwait()
      }
    } catch (e: Exception) {
      if(displaySqlErrors) {
        logger.error("Exception in query: ${e.message}")
        logger.error("Query: ${queryStr}")
        if(args != null) {
          logger.error("Args: ${args.deepToString()}")
        }
      }

      throw e
    }
  }
  fun sendJsonPayload(ctx: RoutingContext, returnObj: JsonObject) {
    returnObj.put("success", true)
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .setStatusCode(200).end(returnObj.encode())
  }

  fun fail400(ctx: RoutingContext, err: Throwable) {
    logger.error("Woops")
    ctx.fail(400, err)
  }

  fun fail500(ctx: RoutingContext, err: Throwable) {
    logger.error("Woops")
    ctx.fail(500, err)
  }
}
