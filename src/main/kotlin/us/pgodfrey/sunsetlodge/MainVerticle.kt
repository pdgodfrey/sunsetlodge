package us.pgodfrey.sunsetlodge

import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var httpPort = 0
  private var httpLogLevel = 0

  override suspend fun start() {
    val env = System.getenv()
    httpPort = env.getOrDefault("HTTP_PORT", "8081").toInt()
    httpLogLevel = env.getOrDefault("HTTP_LOG_LEVEL", "1").toInt()

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())



    router.route().handler(StaticHandler.create());

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
