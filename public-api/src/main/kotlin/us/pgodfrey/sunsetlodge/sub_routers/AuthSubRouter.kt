package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.http.Cookie
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.VertxContextPRNG
import io.vertx.ext.auth.authentication.Credentials
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.AuthSqlQueries
import us.pgodfrey.sunsetlodge.sql.UserSqlQueries
import java.security.InvalidParameterException
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*


class AuthSubRouter(vertx: Vertx, pgPool: PgPool, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pgPool, jwtAuth) {

  private val userSqlQueries = UserSqlQueries()
  private val authSqlQueries = AuthSqlQueries()

  private val jwtAuth: JWTAuth;

  private var JWT_EXPIRE_MINS = 0
  private var JWT_REFRESH_EXPIRE_MINS = 0

  init {
    val env = System.getenv()

    JWT_EXPIRE_MINS = env.getOrDefault("JWT_EXPIRE_MINS", "5").toInt()
    JWT_REFRESH_EXPIRE_MINS = env.getOrDefault("JWT_REFRESH_EXPIRE_MINS", "1440").toInt() //24 hrs

    this.jwtAuth = jwtAuth

    router.post("/reset-password").handler(this::handleResetPassword)
    router.put("/set-password").handler(this::handleSetPassword)
    router.post("/authenticate").handler(this::handleAuthenticate)
    router.post("/refresh").handler(this::handleRefresh)
    router.post("/logout").handler(this::handleLogout)

//    router.post("/").handler(this::handleCreateBooking)
//    router.put("/:id").handler(this::handleUpdateBooking)
//    router.delete("/:id").handler(this::handleDeleteBooking)
  }


  /**
   * Set a reset token and email them a reset link
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/reset-password</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>email</code> - text
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleResetPassword(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val email = ctx.request().getParam("email")
        val users = execQuery(userSqlQueries.getUserByEmail, Tuple.of(email.lowercase()))

        if(users.size() == 0) {
          throw InvalidParameterException("Email address not found")
        } else {
          val user = users.first()
          val userId = user.getInteger("id")

          val resetToken = UUID.randomUUID()

          execQuery(userSqlQueries.setResetPassword, Tuple.of(resetToken.toString(), userId))

          val emailObj = JsonObject()
            .put("recipient_email", user.getString("email"))
            .put("subject", "Sunset Lodge: Password Reset")
            .put("name", user.getString("name"))
            .put("reset_token", resetToken.toString())

          vertx.eventBus().request<Any>("email.send", emailObj) {

            sendJsonPayload(ctx, json {
              obj(

              )
            })
          }
        }
//Thread.sleep(1000)
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }



  /**
   * Set a user's password
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/reset-password</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>email</code> - text
   *   </li>
   *   <li>
   *     <code>reset_token</code> - text
   *   </li>
   *   <li>
   *     <code>password</code> - text
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleSetPassword(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {

        execQuery(userSqlQueries.getUsers).forEach {
          logger.info("USER: ${it.toJson()}")
        }

        val data = ctx.body().asJsonObject()
        val user = execQuery(userSqlQueries.getUserByResetToken, Tuple.of(data.getString("reset_token").lowercase()))

        if(user.size() == 0) {
          throw InvalidParameterException("No user found with this reset token")
        } else {
          val userObj = user.first()

          if(!userObj.getString("email").equals(data.getString("email").lowercase())){
            throw InvalidParameterException("Email address does not match user with this reset token")
          }

          if(userObj.getLocalDateTime("reset_token_expiration").isBefore(LocalDateTime.now())){
            throw InvalidParameterException("Reset token has expired")
          }


          val hash: String = sqlAuthentication.hash(
            "pbkdf2",  // hashing algorithm (OWASP recommended)
            VertxContextPRNG.current().nextString(32),  // secure random salt
            data.getString("password") // password
          )

          execQuery(userSqlQueries.setPassword, Tuple.of(hash, userObj.getInteger("id")))

        }

        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }



  /**
   * Login, get access token & refresh token
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/authenticate</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>email</code> - text
   *   </li>
   *   <li>
   *     <code>password</code> - text
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleAuthenticate(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()
        val authInfo = JsonObject()
          .put("username", data.getString("email"))
          .put("password", data.getString("password"))

        val credentials = UsernamePasswordCredentials(authInfo)

        val authUser = sqlAuthentication.authenticate(credentials)
          .onFailure {
            fail500(ctx, it)
          }
          .await()

        val fullUser = execQuery(userSqlQueries.getUserByEmail, Tuple.of(authUser.principal().getString("username"))).first()

        val fullUserObj = fullUser.toJson()

        issueJwtToken(ctx, fullUserObj)
        issueRefreshToken(ctx, fullUserObj, JWT_REFRESH_EXPIRE_MINS)

        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }



  /**
   * Get access via refresh token
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/refresh</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>refresh_token</code> - text
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleRefresh(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val refreshToken = ctx.request().getParam("refresh_token")

        val credentials = TokenCredentials(refreshToken)

        val authUser = jwtAuth.authenticate(credentials)
          .map {
            it.principal()
          }
          .onFailure {
            fail500(ctx, InvalidParameterException("Invalid refresh token"))
          }
          .await()

        val userId = authUser.getString("sub").toInt()

        val tokenHash = hashString(refreshToken)

        val latestRefreshToken = execQuery(authSqlQueries.getLatestRefreshTokenForUser, Tuple.of(userId)).first()

        if(latestRefreshToken.getString("refresh_token") != tokenHash){
          //Latest token has already been used, invalidate all!
          execQuery(authSqlQueries.revokeRefreshTokensForUser, Tuple.of(userId))
          throw InvalidParameterException("Refresh token is not the latest token")
        } else if(latestRefreshToken.getBoolean("is_used") == true){
          //Latest token has already been used, invalidate all!
          execQuery(authSqlQueries.revokeRefreshTokensForUser, Tuple.of(userId))
          throw InvalidParameterException("Refresh token has already been used")
        } else {
          execQuery(authSqlQueries.setRefreshTokenToUsed, Tuple.of(tokenHash))

          val fullUser = execQuery(userSqlQueries.getUserById, Tuple.of(userId)).first()

          val fullUserObj = fullUser.toJson()

          issueJwtToken(ctx, fullUserObj)
          issueRefreshToken(ctx, fullUserObj, JWT_REFRESH_EXPIRE_MINS)
        }

        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: InvalidParameterException) {
        logger.error(e.printStackTrace())
        fail400(ctx, e)
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }


  /**
   * Logout user, invalidate refresh tokens
   * * requires auth token
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/logout</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>refresh_token</code> - text
   *   </li>
   * </ul>
   * <p>
   *   <b>Path Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>-</code>
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleLogout(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val user = ctx.user().principal()

        val userId = user.getString("sub").toInt()

        ctx.response().removeCookie("refresh-token")
        ctx.response().removeCookie("auth-token")

        execQuery(authSqlQueries.revokeRefreshTokensForUser, Tuple.of(userId))

        sendJsonPayload(ctx, json {
          obj(

          )
        })
      } catch (e: InvalidParameterException) {
        logger.error(e.printStackTrace())
        fail400(ctx, e)
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }

  private fun issueJwtToken(context: RoutingContext, user: JsonObject) {
    logger.info("make auth token")

    val jwtOptions = JWTOptions()
      .setAlgorithm("RS256")
      .setExpiresInMinutes(JWT_EXPIRE_MINS)
      .setIssuer("sunsetlodge-api")
      .setSubject("${user.getInteger("id")}")

    val claims = JsonObject()
      .put("name", user.getString("name"))


    val token = jwtAuth.generateToken(claims, jwtOptions)

    val cookie = Cookie.cookie("auth-token", token)
    cookie.setHttpOnly(true)
    cookie.setMaxAge((JWT_EXPIRE_MINS * 60).toLong())

    context.response().addCookie(cookie)
  }

  private suspend fun issueRefreshToken(context: RoutingContext, user: JsonObject, expirationMinutes: Int) {
    logger.info("make refresh token")

    val expiration = LocalDateTime.now().plusMinutes(expirationMinutes.toLong())

    val jwtOptions = JWTOptions()
      .setAlgorithm("RS256")
      .setExpiresInMinutes(expirationMinutes)
      .setIssuer("sunsetlodge-api")
      .setSubject("${user.getInteger("id")}")

    val claims = JsonObject()
      .put("name", user.getString("name"))


    val token = jwtAuth.generateToken(claims, jwtOptions)

    val hash = hashString(token)

    execQuery(authSqlQueries.insertRefreshToken, Tuple.of(hash, expiration, user.getInteger("id")))

    val cookie = Cookie.cookie("refresh-token", token)
    cookie.setHttpOnly(true)
    cookie.setMaxAge((expirationMinutes * 60).toLong())
    cookie.setSecure(true)

    context.response().addCookie(cookie)

  }

  private fun hashString(input: String): String {
    val HEX_CHARS = "0123456789ABCDEF"
    val bytes = MessageDigest
      .getInstance("SHA-256")
      .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
      val i = it.toInt()
      result.append(HEX_CHARS[i shr 4 and 0x0f])
      result.append(HEX_CHARS[i and 0x0f])
    }

    return result.toString()
  }
}
