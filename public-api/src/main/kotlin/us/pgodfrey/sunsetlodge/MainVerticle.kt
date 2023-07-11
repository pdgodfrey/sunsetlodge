package us.pgodfrey.sunsetlodge

import io.vertx.core.http.HttpMethod
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.auth.sqlclient.SqlAuthentication
import io.vertx.ext.auth.sqlclient.SqlAuthenticationOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import us.pgodfrey.sunsetlodge.handler.SunsetAuthHandler
import us.pgodfrey.sunsetlodge.handler.impl.JsonBodyLoginHandlerImpl
import us.pgodfrey.sunsetlodge.sub_routers.*
import us.pgodfrey.sunsetlodge.verticles.EmailerVerticle
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths


class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var httpPort = 0
  private var httpLogLevel = 0

  private lateinit var baseSubRouter: BaseSubRouter

  private lateinit var pgPool: PgPool


  private var uploadsDir = ""

  private lateinit var sqlAuthentication: SqlAuthentication

  override suspend fun start() {
    val env = System.getenv()
    httpPort = env.getOrDefault("HTTP_PORT", "8081").toInt()
    httpLogLevel = env.getOrDefault("HTTP_LOG_LEVEL", "1").toInt()

    val dbHost = env.getOrDefault("PG_HOST", "localhost")
    val dbName = env.getOrDefault("PG_DB", "sunsetlodge")
    val dbUser = env.getOrDefault("PG_USER", "sunset")
    val dbPass = env.getOrDefault("PG_PASS", "abc123")

    uploadsDir = env.getOrDefault("UPLOADS_DIR", "uploads")


    val pgOptions = pgConnectOptionsOf(host = dbHost, database = dbName, user = dbUser, password = dbPass)
    pgPool = PgPool.pool(vertx, pgOptions, PoolOptions())


    val emailVerticle = EmailerVerticle()
    vertx.deployVerticle(emailVerticle).await()

    baseSubRouter = BaseSubRouter(vertx, pgPool)

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create())

    router.route().handler(StaticHandler.create());

    val store: SessionStore = LocalSessionStore.create(vertx)

    router.route().handler(SessionHandler.create(store).setNagHttps(false))

    val sqlOptions = SqlAuthenticationOptions()

    sqlOptions.setAuthenticationQuery("SELECT password FROM users WHERE email ilike $1")

    sqlAuthentication = SqlAuthentication.create(pgPool, sqlOptions)


    router.route("/*").handler { ctx: RoutingContext ->

      logger.info(
        "=====" + ctx.session().id()
      )
      logger.info(
        "=====" + ctx.request().method() + ": " + ctx.normalizedPath() + " : " + ctx.request().absoluteURI()
      )
      logger.info(ctx.request().cookies().size.toString())
      ctx.request().cookies().forEach {
        logger.info("${it.name} : ${it.value}")
      }

      ctx.next()
    }

    val allowedHeaders: MutableSet<String> = HashSet()
    allowedHeaders.add("x-requested-with")
    allowedHeaders.add("Access-Control-Allow-Origin")
    allowedHeaders.add("origin")
    allowedHeaders.add("Content-Type")
    allowedHeaders.add("accept")
    allowedHeaders.add("Authorization")

    router.route().handler(
      CorsHandler.create()
        .addOrigin("http://localhost:3000")
        .allowCredentials(true)
        .allowedHeaders(allowedHeaders)
        .allowedMethod(HttpMethod.POST)
        .allowedMethod(HttpMethod.GET)
        .allowedMethod(HttpMethod.PUT)
        .allowedMethod(HttpMethod.DELETE)
        .allowedMethod(HttpMethod.OPTIONS)
    )


    router.get("/readyz").handler(baseSubRouter::readinessCheck)
    router.get("/healthz").handler(this::healthCheck)

    val jsonBodyLoginHandler = JsonBodyLoginHandlerImpl(sqlAuthentication, "email", "password", null, null)

    val sunsetAuthHandler = SunsetAuthHandler.create(sqlAuthentication)

    router.route("/api/auth/authenticate").handler(jsonBodyLoginHandler);


    router.route("/api/auth/user").handler(sunsetAuthHandler);

    router.route("/api/auth*").subRouter(AuthSubRouter(vertx, pgPool, sqlAuthentication).getSubRouter());


//    router.route("/api*").handler(RedirectAuthHandler.create(sqlAuthentication, "/auth/login"));


    router.route("/api*").handler(sunsetAuthHandler);

    router.route("/api/bookings*").subRouter(BookingsSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/galleries*").subRouter(GalleriesSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/images*").subRouter(ImagesSubRouter(vertx, pgPool, uploadsDir).getSubRouter());
    router.route("/api/rates*").subRouter(RatesSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/seasons*").subRouter(SeasonsSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/users*").subRouter(UsersSubRouter(vertx, pgPool).getSubRouter());
    router.route("/api/*").subRouter(MiscSubRouter(vertx, pgPool).getSubRouter());

    router.get("/gallery-images/*").handler(this::serveUpload)

    router.route().subRouter(PagesSubRouter(vertx, pgPool).getSubRouter());

    router.route().failureHandler { failureRoutingContext ->
      logger.info("failure handler")
      val statusCode: Int = failureRoutingContext.statusCode()

      logger.info("status code ${statusCode}")
      val response = failureRoutingContext.response()
      failureRoutingContext.failure().printStackTrace()
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

  private fun serveUpload(ctx: RoutingContext) {
    try {
      var path = ctx.normalizedPath()
      path = path.replace("/gallery-images", "")

      if(!vertx.fileSystem().existsBlocking("$uploadsDir$path")) {
        ctx.fail(404, FileNotFoundException("File does not exist"))
      } else {

        ctx
          .response()
          .sendFile("$uploadsDir$path");
      }

    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      ctx.fail(500, e)
    }
  }


  private fun readPemFile(file: String): String {
    val env = System.getenv()
    val pemPath = env.getOrDefault("PEM_PATH", "pem_keys")
    var path = Paths.get(pemPath, file)
    logger.info("Path: ${path.toAbsolutePath()}")
    logger.info("Exists?: ${path.toFile().exists()}")
    if (!path.toFile().exists()) {
      throw FileNotFoundException("File '${file}' does not exist!")
    }
    return java.lang.String.join("\n", Files.readAllLines(path, StandardCharsets.UTF_8))
  }

}
