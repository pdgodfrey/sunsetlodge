package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.VertxContextPRNG
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.sqlclient.SqlAuthentication
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.AuthSqlQueries
import us.pgodfrey.sunsetlodge.sql.UserSqlQueries
import java.security.InvalidParameterException
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDateTime
import java.util.*


class AuthSubRouter(vertx: Vertx, pool: Pool, sqlAuthentication: SqlAuthentication, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pool, jwtAuth) {

  private val userSqlQueries = UserSqlQueries()
  private val authSqlQueries = AuthSqlQueries()

  private val jwtAuth: JWTAuth

  private var JWT_EXPIRE_MINS = 0
  private var JWT_REFRESH_EXPIRE_MINS = 0

  private val utcZone = Clock.systemUTC()

  private var sqlAuthentication: SqlAuthentication

  init {
    val env = System.getenv()

    JWT_EXPIRE_MINS = env.getOrDefault("JWT_EXPIRE_MINS", "5").toInt()
    JWT_REFRESH_EXPIRE_MINS = env.getOrDefault("JWT_REFRESH_EXPIRE_MINS", "1440").toInt() //24 hrs

    this.jwtAuth = jwtAuth

    router.get("/user").coroutineHandler(this::handleGetUser)
    router.post("/authenticate").coroutineHandler(this::handleAuthenticate)
    router.post("/reset-password").coroutineHandler(this::handleResetPassword)
    router.post("/set-password").coroutineHandler(this::handleSetPassword)
    router.post("/refresh").coroutineHandler(this::handleRefresh)
    router.post("/logout").coroutineHandler(this::handleLogout)

    this.sqlAuthentication = sqlAuthentication
  }


  /**
   * Get Logged In USer
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/user</code>
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
  private suspend fun handleGetUser(ctx: RoutingContext) {
    try {
      val ctxUser = ctx.user().principal()

      val user = execQuery(userSqlQueries.getUserById, Tuple.of(ctxUser.getString("sub").toInt())).first().toJson()

      val returnUser = JsonObject()
        .put("name", user.getString("name"))
        .put("email", user.getString("email"))
        .put("role_name", user.getString("role_name"))

      sendJsonPayload(ctx, json {
        obj(
          "user" to returnUser
        )
      })
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
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
   *     <li>
   *       <code>refresh_token</code> - text
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleAuthenticate(ctx: RoutingContext) {
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
        .coAwait()

      val fullUser = execQuery(userSqlQueries.getUserByEmail, Tuple.of(authUser.principal().getString("username"))).first()

      val fullUserObj = fullUser.toJson()

      val authToken = issueJwtToken(fullUserObj)
      val refreshToken = issueRefreshToken(fullUserObj, JWT_REFRESH_EXPIRE_MINS)

      sendJsonPayload(ctx, json {
        obj(
          "token" to authToken,
          "refresh_token" to refreshToken
        )
      })
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
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
   *     <code>refresh_token</code> - text
   *   </li>
   * </ul>
   * <p>
   *   <b>Json Return Body:</b>
   * </p>
   *   <ul>
   *     <li>
   *       <code>success</code> - boolean
   *     </li>
   *     <li>
   *       <code>refresh_token</code> - text
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleRefresh(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()
      val refreshToken = data.getString("refresh_token")

      val credentials = TokenCredentials(refreshToken)

      logger.info("refresh token ${refreshToken}")

      val authUser = jwtAuth.authenticate(credentials)
        .map {
          it.principal()
        }
        .onFailure {
          fail500(ctx, InvalidParameterException("Invalid refresh token"))
        }
        .coAwait()

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

        val authToken = issueJwtToken(fullUserObj)
        val newRefreshToken = issueRefreshToken(fullUserObj, JWT_REFRESH_EXPIRE_MINS)

        sendJsonPayload(ctx, json {
          obj(
            "token" to authToken,
            "refresh_token" to newRefreshToken
          )
        })
      }

    } catch (e: InvalidParameterException) {
      logger.error(e.printStackTrace())
      fail400(ctx, e)
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
    }
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
  private suspend fun handleResetPassword(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()
      logger.info("DATA ${data}")
      val email = data.getString("email")
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
          .put("template", "password-reset.hbs")

        vertx.eventBus().request<Any>("email.send", emailObj) {

          sendJsonPayload(ctx, json {
            obj(

            )
          })
        }
      }
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
    }
  }



  /**
   * Set a user's password
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/auth/set-password</code>
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
  private suspend fun handleSetPassword(ctx: RoutingContext) {
    try {

      val data = ctx.body().asJsonObject()
      val user = execQuery(userSqlQueries.getUserByResetToken, Tuple.of(data.getString("reset_token").lowercase()))

      if(user.size() == 0) {
        throw InvalidParameterException("No user found with this reset token")
      } else {
        val userObj = user.first()

        if(!userObj.getString("email").equals(data.getString("email").lowercase())){
          throw InvalidParameterException("Email address does not match user with this reset token")
        }

        logger.info("${userObj.getLocalDateTime("reset_token_expiration")}")
        logger.info("${LocalDateTime.now(utcZone)}")
        if(userObj.getLocalDateTime("reset_token_expiration").isBefore(LocalDateTime.now(utcZone))){
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
  private suspend fun handleLogout(ctx: RoutingContext) {
    try {
      val user = ctx.user().principal()

      val userId = user.getString("sub").toInt()

      ctx.response().removeCookie("auth-token")

      execQuery(authSqlQueries.revokeRefreshTokensForUser, Tuple.of(userId))

      ctx.clearUser()

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

  private fun issueJwtToken(user: JsonObject): String {

    val jwtOptions = JWTOptions()
      .setAlgorithm("RS256")
      .setExpiresInMinutes(JWT_EXPIRE_MINS)
      .setIssuer("sunsetlodge-api")
      .setSubject("${user.getInteger("id")}")

    val claims = JsonObject()
      .put("name", user.getString("name"))
      .put("role", user.getString("role_name"))


    val token = jwtAuth.generateToken(claims, jwtOptions)

    return token
  }

  private suspend fun issueRefreshToken(user: JsonObject, expirationMinutes: Int): String {

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

    return token

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
