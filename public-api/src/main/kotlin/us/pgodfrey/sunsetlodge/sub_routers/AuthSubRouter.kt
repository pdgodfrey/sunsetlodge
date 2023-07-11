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
import io.vertx.ext.auth.sqlclient.SqlAuthentication
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
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*


class AuthSubRouter(vertx: Vertx, pgPool: PgPool, sqlAuthentication: SqlAuthentication) : BaseSubRouter(vertx, pgPool) {

  private val userSqlQueries = UserSqlQueries()
  private val authSqlQueries = AuthSqlQueries()

  private var JWT_EXPIRE_MINS = 0
  private var JWT_REFRESH_EXPIRE_MINS = 0

  private val utcZone = Clock.systemUTC()

  private lateinit var sqlAuthentication: SqlAuthentication

  init {
    val env = System.getenv()

    JWT_EXPIRE_MINS = env.getOrDefault("JWT_EXPIRE_MINS", "5").toInt()
    JWT_REFRESH_EXPIRE_MINS = env.getOrDefault("JWT_REFRESH_EXPIRE_MINS", "1440").toInt() //24 hrs

    router.get("/user").handler(this::handleGetUser)
    router.post("/reset-password").handler(this::handleResetPassword)
    router.post("/set-password").handler(this::handleSetPassword)
    router.post("/logout").handler(this::handleLogout)

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
  fun handleGetUser(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val ctxUser = ctx.user()

        val user = execQuery(userSqlQueries.getUserByEmail, Tuple.of(ctx.user().principal().getString("username"))).first().toJson()

        val returnUser = JsonObject()
          .put("name", user.getString("name"))
          .put("email", user.getString("email"))

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
  fun handleSetPassword(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
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
  }



//  /**
//   * Login, get access token & refresh token
//   *
//   * <p>
//   *   <b>Method:</b> <code>POST</code>
//   * </p>
//   * <p>
//   *   <b>Path:</b> <code>/auth/authenticate</code>
//   * </p>
//   * <p>
//   *   <b>Query Params:</b>
//   * </p>
//   *
//   * <ul>
//   *   <li>
//   *     <code>-</code>
//   *   </li>
//   * </ul>
//   * <p>
//   *   <b>Path Params:</b>
//   * </p>
//   *
//   * <ul>
//   *   <li>
//   *     <code>-</code>
//   *   </li>
//   * </ul>
//   * <p>
//   *   <b>Body Params:</b>
//   * </p>
//   *
//   * <ul>
//   *   <li>
//   *     <code>email</code> - text
//   *   </li>
//   *   <li>
//   *     <code>password</code> - text
//   *   </li>
//   * </ul>
//   * <p>
//   *   <b>Json Return Body:</b>
//   * </p>
//   *   <ul>
//   *     <li>
//   *       <code>success</code> - boolean
//   *     </li>
//   *   </ul>
//   *
//   *
//   * @param context RoutingContext
//   */
//  fun handleAuthenticate(ctx: RoutingContext) {
//    GlobalScope.launch(vertx.dispatcher()) {
//      try {
//        val data = ctx.body().asJsonObject()
//        val authInfo = JsonObject()
//          .put("username", data.getString("email"))
//          .put("password", data.getString("password"))
//
//        val credentials = UsernamePasswordCredentials(authInfo)
//
//        val authUser = sqlAuthentication.authenticate(credentials)
//          .onFailure {
//            fail500(ctx, it)
//          }
//          .await()
//
//        val fullUser = execQuery(userSqlQueries.getUserByEmail, Tuple.of(authUser.principal().getString("username"))).first()
//
//        val fullUserObj = fullUser.toJson()
//
//        val session = ctx.session()
//        session.put("user_id", fullUser.getInteger("id"))
//
//        sendJsonPayload(ctx, json {
//          obj(
//
//          )
//        })
//      } catch (e: Exception) {
//        logger.error(e.printStackTrace())
//        fail500(ctx, e)
//      }
//    }
//  }


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
        ctx.clearUser()
//        ctx.session().destroy()

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
}
