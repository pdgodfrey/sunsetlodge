package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.AuthSqlQueries
import us.pgodfrey.sunsetlodge.sql.UserSqlQueries
import java.security.InvalidParameterException
import java.util.*
import kotlin.collections.ArrayList


class UsersSubRouter(vertx: Vertx, pool: Pool, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pool, jwtAuth) {

  private val userSqlQueries = UserSqlQueries()
  private val authSqlQueries = AuthSqlQueries()
  private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"

  init {

    router.get("/").coroutineHandler(this::handleGetUsers)
    router.post("/").coroutineHandler(this::handleCreateUser)
    router.post("/reset-password").coroutineHandler(this::handleResetPassword)
    router.put("/:id").coroutineHandler(this::handleUpdateUser)
    router.delete("/:id").coroutineHandler(this::handleDeleteUser)
  }


  /**
   * Get a list of users
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/users</code>
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
   *     <li>
   *       <code>rows</code> - array
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>role_id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>email</code> - text</li>
   *           <li><code>role_name</code> - text</li>
   *           <li><code>created_at</code> - text (yyyy-mm-dd'T'HH:mm:ss.SSSSSS)</li>
   *           <li><code>updated_at</code> - text (yyyy-mm-dd'T'HH:mm:ss.SSSSSS)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleGetUsers(ctx: RoutingContext) {
    try {
      val users = execQuery(userSqlQueries.getUsers)

      sendJsonPayload(ctx, json {
        obj(
          "rows" to users.map { it.toJson() }
        )
      })
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
    }
  }


  /**
   * Create a user
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/users</code>
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
   *   <b>Body Params (Json Format):</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>name</code> - text
   *   </li>
   *   <li>
   *     <code>email</code> - text
   *   </li>
   *   <li>
   *     <code>role_id</code> - integer
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
   *       <code>data</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>email</code> - text</li>
   *           <li><code>role_id</code> - integer</li>
   *           <li><code>role_name</code> - text</li>
   *           <li><code>created_at</code> - text (yyyy-mm-dd'T'HH:mm:ss.SSSSSS)</li>
   *           <li><code>updated_at</code> - text (yyyy-mm-dd'T'HH:mm:ss.SSSSSS)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleCreateUser(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()

      validateUserData(data)

      val params = Tuple.tuple()
      params.addString(data.getString("name"))
      params.addString(data.getString("email").lowercase())
      params.addInteger(data.getInteger("role_id"))

      val insertedUser = execQuery(userSqlQueries.insertUser, params)

      val user = execQuery(userSqlQueries.getUserById, Tuple.of(insertedUser.first().getInteger("id")))

      sendJsonPayload(ctx, json {
        obj(
          "data" to user.first().toJson()
        )
      })
    } catch (e: InvalidParameterException) {
      logger.error(e.printStackTrace())
      fail400(ctx, e)
    } catch (e: IllegalArgumentException) {
      logger.error(e.printStackTrace())
      fail400(ctx, e)
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
    }
  }



  /**
   * Reset a users password
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/users/reset-password</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>id</code> - integer
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
   *   <b>Body Params (Json Format):</b>
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
  private suspend fun handleResetPassword(ctx: RoutingContext) {
    try {
      val id = ctx.request().getParam("id").toInt()
      val users = execQuery(userSqlQueries.getUserById, Tuple.of(id))
      val user = users.first()

      val resetToken = UUID.randomUUID()

      execQuery(userSqlQueries.setResetPassword, Tuple.of(resetToken.toString(), id))

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
    } catch (e: InvalidParameterException) {
      logger.error(e.printStackTrace())
      fail400(ctx, e)
    } catch (e: IllegalArgumentException) {
      logger.error(e.printStackTrace())
      fail400(ctx, e)
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
    }
  }

  /**
   * Update a User
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/users/:id</code>
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
   *     <code>id</code> - integer
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params (Json Format):</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>id</code> - integer
   *   </li>
   *   <li>
   *     <code>name</code> - text
   *   </li>
   *   <li>
   *     <code>email</code> - text
   *   </li>
   *   <li>
   *     <code>role_id</code> - integer
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
   *       <code>data</code> - object
   *       <ul>
   *           <li><code>id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>email</code> - text</li>
   *           <li><code>role_id</code> - integer</li>
   *           <li><code>role_name</code> - text</li>
   *           <li><code>created_at</code> - text (yyyy-mm-dd'T'HH:mm:ss.SSSSSS)</li>
   *           <li><code>updated_at</code> - text (yyyy-mm-dd'T'HH:mm:ss.SSSSSS)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  private suspend fun handleUpdateUser(ctx: RoutingContext) {
    try {
      val data = ctx.body().asJsonObject()

      validateUserData(data)

      val id = ctx.request().getParam("id").toInt()

      val params = Tuple.tuple()
      params.addString(data.getString("name"))
      params.addString(data.getString("email"))
      params.addInteger(data.getInteger("role_id"))
      params.addInteger(id)

      execQuery(userSqlQueries.updateUser, params)

      val user = execQuery(userSqlQueries.getUserById, Tuple.of(id))

      sendJsonPayload(ctx, json {
        obj(
          "data" to user.first().toJson()
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


  /**
   * Delete a User
   *
   * <p>
   *   <b>Method:</b> <code>DELETE</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/users/:id</code>
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
   *     <code>id</code> - integer
   *   </li>
   * </ul>
   * <p>
   *   <b>Body Params (Json Format):</b>
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
  private suspend fun handleDeleteUser(ctx: RoutingContext) {
    try {
      val id = ctx.request().getParam("id").toInt()

      execQuery(authSqlQueries.revokeRefreshTokensForUser, Tuple.of(id))
      execQuery(userSqlQueries.deleteUser, Tuple.of(id))

      sendJsonPayload(ctx, json {
        obj(

        )
      })
    } catch (e: Exception) {
      logger.error(e.printStackTrace())
      fail500(ctx, e)
    }
  }

  private fun validateUserData(data: JsonObject?){
    if (data == null) {
      throw InvalidParameterException("No request body")
    } else {
      val invalidFields = ArrayList<String>()
      if (!data.containsKey("name") || data.getString("name").isNullOrBlank()) {
        invalidFields.add("Name is a required field")
      }
      if (!data.containsKey("email") || data.getString("email").isNullOrBlank()) {
        invalidFields.add("Email is a required field")
      } else if(!isValidEmail(data.getString("email"))) {
        invalidFields.add("Email is invalid")
      }
      if (!data.containsKey("role_id") || data.getInteger("role_id") == null) {
        invalidFields.add("Role is a required field")
      }

      if(invalidFields.size > 0){
        throw InvalidParameterException(invalidFields.joinToString(", "))
      }
    }
  }

  private fun isValidEmail(email: String): Boolean {
    return email.lowercase().matches(emailRegex.toRegex())
  }
}
