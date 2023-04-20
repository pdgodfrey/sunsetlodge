package us.pgodfrey.sunsetlodge

import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import us.pgodfrey.sunsetlodge.sub_routers.PagesSubRouter

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

//    baseSubRouter = BaseSubRouter(vertx, pgPool)

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())



    router.route().handler(StaticHandler.create());

    router.route("/*").subRouter(PagesSubRouter(vertx, pgPool).getSubRouter());

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
}
