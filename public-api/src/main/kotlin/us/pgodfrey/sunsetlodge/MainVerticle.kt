package us.pgodfrey.sunsetlodge

import io.vertx.core.http.HttpMethod
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.sqlclient.SqlAuthentication
import io.vertx.ext.auth.sqlclient.SqlAuthenticationOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.PoolOptions
import us.pgodfrey.sunsetlodge.sub_routers.*
import us.pgodfrey.sunsetlodge.verticles.EmailerVerticle
import java.io.FileNotFoundException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths


class MainVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var httpPort = 0
  private var httpLogLevel = 0

  private lateinit var baseSubRouter: BaseSubRouter

  private lateinit var jwtAuth: JWTAuth
  private lateinit var jwtHandler: JWTAuthHandler

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
    val dbMaxPoolSize = env.getOrDefault("PG_MAX_POOL_SIZE", "4").toInt()

    uploadsDir = env.getOrDefault("UPLOADS_DIR", "uploads")


    val pgOptions = pgConnectOptionsOf(host = dbHost, database = dbName, user = dbUser, password = dbPass)
    val poolOptions = PoolOptions()
      .setMaxSize(dbMaxPoolSize)
      .setShared(true)
      .setName("sunset-pool")
    val pool = PgBuilder.pool().with(poolOptions).connectingTo(pgOptions).using(vertx).build()


    val emailVerticle = EmailerVerticle()
    vertx.deployVerticle(emailVerticle).coAwait()


    val publicKey = readPemFile("public_key.pem")
    val privateKey = readPemFile("private_key.pem")

    jwtAuth = JWTAuth.create(
      vertx, JWTAuthOptions()
        .addPubSecKey(
          PubSecKeyOptions()
            .setAlgorithm("RS256")
            .setBuffer(publicKey)
        )
        .addPubSecKey(
          PubSecKeyOptions()
            .setAlgorithm("RS256")
            .setBuffer(privateKey)
        )
    )
    jwtHandler = JWTAuthHandler.create(jwtAuth)

    baseSubRouter = BaseSubRouter(vertx, pool, jwtAuth)

    val router = Router.router(vertx)

    router.route().handler(BodyHandler.create().setBodyLimit(52428800L))

    router.route().handler(StaticHandler.create().setCachingEnabled(false))

    val sqlOptions = SqlAuthenticationOptions()

    sqlOptions.setAuthenticationQuery("SELECT password FROM users WHERE email ilike $1")

    sqlAuthentication = SqlAuthentication.create(pool, sqlOptions)


    router.route("/*").handler { ctx: RoutingContext ->

      logger.info(
        "=====" + ctx.request().method() + ": " + ctx.normalizedPath() + " : " + ctx.request().absoluteURI()
      )
////      ctx.request().cookies().forEach {
////        if(it.name.equals("auth-token")){
////          ctx.request().headers().add("Authorization", "Bearer ${it.value}")
////        }
////      }
//
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
        .addOrigin("http://localhost:8081")
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

    router.route("/api/auth/user").handler(jwtHandler);
    router.route("/api/auth/logout").handler(jwtHandler);

    router.route("/api/auth*").subRouter(AuthSubRouter(vertx, pool, sqlAuthentication, jwtAuth).getSubRouter());

    router.route("/api*").handler(jwtHandler);

    router.route("/api/bookings*").subRouter(BookingsSubRouter(vertx, pool, jwtAuth).getSubRouter());
    router.route("/api/galleries*").subRouter(GalleriesSubRouter(vertx, pool, jwtAuth).getSubRouter());
    router.route("/api/images*").subRouter(ImagesSubRouter(vertx, pool, uploadsDir, jwtAuth).getSubRouter());
    router.route("/api/rates*").subRouter(RatesSubRouter(vertx, pool, jwtAuth).getSubRouter());
    router.route("/api/seasons*").subRouter(SeasonsSubRouter(vertx, pool, jwtAuth).getSubRouter());
    router.route("/api/users*").subRouter(UsersSubRouter(vertx, pool, jwtAuth).getSubRouter());
    router.route("/api/*").subRouter(MiscSubRouter(vertx, pool, jwtAuth).getSubRouter());

    router.get("/gallery-images/*").handler(this::serveUpload)

    router.route().subRouter(PagesSubRouter(vertx, pool, jwtAuth).getSubRouter());

    //Catch direct access to admin routes
    router.get("/admin/*").handler { ctx ->
      ctx
        .response()
        .sendFile("webroot/admin/index.html");
    }

    router.route().failureHandler { failureRoutingContext ->
      logger.info("failure handler ${failureRoutingContext.request().path()}")
      val statusCode: Int = failureRoutingContext.statusCode()

      logger.info("status code ${statusCode}")
      val response = failureRoutingContext.response()

      var message: String?
      if(statusCode == 413) {
        message = "One or more uploaded files is too big"
      } else {
        message = failureRoutingContext.failure().message

        failureRoutingContext.failure().printStackTrace()
        if(failureRoutingContext.failure().cause != null){
          message = "${failureRoutingContext.failure().cause!!.message}\n${message}"
        }
      }
      response.setStatusCode(statusCode).end(message)
    }

    val httpServer = vertx.createHttpServer()
      .requestHandler(router)
      .listen(httpPort)
      .coAwait()

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
      var path = URLDecoder.decode(ctx.normalizedPath(), "UTF-8")
      path = path.replace("/gallery-images", "")
      logger.info("PATH ${path}")

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
    val path = Paths.get(pemPath, file)
    logger.info("Path: ${path.toAbsolutePath()}")
    logger.info("Exists?: ${path.toFile().exists()}")
    if (!path.toFile().exists()) {
      throw FileNotFoundException("File '${file}' does not exist!")
    }
    return java.lang.String.join("\n", Files.readAllLines(path, StandardCharsets.UTF_8))
  }

}
