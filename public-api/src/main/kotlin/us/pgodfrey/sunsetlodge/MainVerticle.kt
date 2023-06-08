package us.pgodfrey.sunsetlodge

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import us.pgodfrey.sunsetlodge.sub_routers.*


class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var httpPort = 0
  private var httpLogLevel = 0

  lateinit var baseSubRouter: BaseSubRouter

  private lateinit var pgPool: PgPool

  override suspend fun start() {
    val env = System.getenv()
    httpPort = env.getOrDefault("HTTP_PORT", "8081").toInt()
    httpLogLevel = env.getOrDefault("HTTP_LOG_LEVEL", "1").toInt()

    val dbHost = env.getOrDefault("PG_HOST", "localhost")
    val dbName = env.getOrDefault("PG_DB", "sunsetlodge")
    val dbUser = env.getOrDefault("PG_USER", "sunset")
    val dbPass = env.getOrDefault("PG_PASS", "abc123")


    val pgOptions = pgConnectOptionsOf(host = dbHost, database = dbName, user = dbUser, password = dbPass)
    pgPool = PgPool.pool(vertx, pgOptions, PoolOptions())

    baseSubRouter = BaseSubRouter(vertx, pgPool)

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route().handler(StaticHandler.create());


//    router.route("/*").handler { ctx: RoutingContext ->
//
//      logger.info(
//        "=====" + ctx.request().method() + ": " + ctx.normalizedPath() + " : " + ctx.request().absoluteURI()
//      )
//
//      ctx.next()
//    }

    router.get("/readyz").handler(baseSubRouter::readinessCheck)
    router.get("/healthz").handler(this::healthCheck)


    router.route("/api/bookings*").subRouter(BookingsSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/rates*").subRouter(RatesSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/seasons*").subRouter(SeasonsSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/*").subRouter(MiscSubRouter(vertx, pgPool).getSubRouter());

    router.route().subRouter(PagesSubRouter(vertx, pgPool).getSubRouter());

    router.route().failureHandler { failureRoutingContext ->
      val statusCode: Int = failureRoutingContext.statusCode()

      val response = failureRoutingContext.response()
      response.setStatusCode(statusCode).end(failureRoutingContext.failure().message)
    }

    val httpServer = vertx.createHttpServer()
      .requestHandler(router)
      .listen(httpPort)
      .await()

    try {
      logger.info("HTTP server deployed on port: ${httpServer.actualPort()}")
    } catch (e: Exception) {
      logger.info("Failed to deploy HTTP server on port ${httpServer.actualPort()} with error ${e.message}")
    }
  }

  private val okStatus = json { obj("status" to "UP") }
  private fun healthCheck(ctx: RoutingContext) {
    //logger.info("Health check")
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(okStatus.encode())

  }
}
