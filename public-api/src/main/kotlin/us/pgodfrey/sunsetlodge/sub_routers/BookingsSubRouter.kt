package us.pgodfrey.sunsetlodge.sub_routers

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.sql.BookingSqlQueries
import us.pgodfrey.sunsetlodge.sql.SeasonSqlQueries
import java.security.InvalidParameterException
import java.time.LocalDate


class BookingsSubRouter(vertx: Vertx, pgPool: PgPool) : BaseSubRouter(vertx, pgPool) {

  private val bookingSqlQueries = BookingSqlQueries();
  private val seasonSqlQueries = SeasonSqlQueries();

  init {

    router.get("/").handler(this::handleGetBookingsForSeason)
    router.post("/").handler(this::handleCreateBooking)
    router.put("/:id").handler(this::handleUpdateBooking)
    router.delete("/:id").handler(this::handleDeleteBooking)
  }


  /**
   * Get a list of bookings for a given season
   *
   * <p>
   *   <b>Method:</b> <code>GET</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/bookings</code>
   * </p>
   * <p>
   *   <b>Query Params:</b>
   * </p>
   *
   * <ul>
   *   <li>
   *     <code>season_id</code> - integer
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
   *           <li><code>season_id</code> - integer</li>
   *           <li><code>name</code> - text</li>
   *           <li><code>start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>end_date</code> - text (yyyy-mm-dd)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleGetBookingsForSeason(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("season_id").toInt()
        val bookings = execQuery(bookingSqlQueries.getBookingsForSeason, Tuple.of(id))

        sendJsonPayload(ctx, json {
          obj(
            "rows" to bookings.map { it.toJson() }
          )
        })
      } catch (e: Exception) {
        logger.error(e.printStackTrace())
        fail500(ctx, e)
      }
    }
  }


  /**
   * Create a booking for a season
   *
   * <p>
   *   <b>Method:</b> <code>POST</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/bookings</code>
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
   *     <code>season_id</code> - integer
   *   </li>
   *   <li>
   *     <code>name</code> - text
   *   </li>
   *   <li>
   *     <code>start_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>end_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>building_ids</code> - integer array
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
   *           <li><code>start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>end_date</code> - text (yyyy-mm-dd)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleCreateBooking(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()

        validateBookingData(data)

        validateBookingDates(data)

        val params = Tuple.tuple()
        params.addInteger(data.getInteger("season_id"))
        params.addString(data.getString("name"))
        params.addLocalDate(LocalDate.parse(data.getString("start_date")))
        params.addLocalDate(LocalDate.parse(data.getString("end_date")))

        val booking = execQuery(bookingSqlQueries.createBooking, params)

        val buildingIds = data.getJsonArray("building_ids")

        buildingIds.forEach {
          val buildingId = it as Int
          execQuery(bookingSqlQueries.createBookingBuilding,
            Tuple.of(booking.first().getInteger("id"), buildingId))
        }


        sendJsonPayload(ctx, json {
          obj(
            "data" to booking.first().toJson()
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
  }


  /**
   * Update a Booking
   *
   * <p>
   *   <b>Method:</b> <code>PUT</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/bookings/:id</code>
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
   *     <code>start_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>end_date</code> - text (yyyy-mm-dd)
   *   </li>
   *   <li>
   *     <code>building_ids</code> - integer array
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
   *           <li><code>start_date</code> - text (yyyy-mm-dd)</li>
   *           <li><code>end_date</code> - text (yyyy-mm-dd)</li>
   *       </ul>
   *     </li>
   *   </ul>
   *
   *
   * @param context RoutingContext
   */
  fun handleUpdateBooking(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val data = ctx.body().asJsonObject()

        validateBookingData(data)

        validateBookingDates(data)

        val id = ctx.request().getParam("id").toInt()

        val params = Tuple.tuple()
        params.addString(data.getString("name"))
        params.addLocalDate(LocalDate.parse(data.getString("start_date")))
        params.addLocalDate(LocalDate.parse(data.getString("end_date")))
        params.addInteger(id)

        val booking = execQuery(bookingSqlQueries.updateBooking, params)

        execQuery(bookingSqlQueries.deleteBookingBuildingForBooking, Tuple.of(id))

        val buildingIds = data.getJsonArray("building_ids")

        buildingIds.forEach {
          val buildingId = it as Int
          execQuery(bookingSqlQueries.createBookingBuilding,
            Tuple.of(booking.first().getInteger("id"), buildingId))
        }

        sendJsonPayload(ctx, json {
          obj(
            "data" to booking.first().toJson()
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
   * Delete a Booking
   *
   * <p>
   *   <b>Method:</b> <code>DELETE</code>
   * </p>
   * <p>
   *   <b>Path:</b> <code>/api/bookings/:id</code>
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
  fun handleDeleteBooking(ctx: RoutingContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val id = ctx.request().getParam("id").toInt()

        execQuery(bookingSqlQueries.deleteBookingBuildingForBooking, Tuple.of(id))
        execQuery(bookingSqlQueries.deleteBooking, Tuple.of(id))

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

  fun validateBookingData(data: JsonObject?){
    if (data == null) {
      throw InvalidParameterException("No request body")
    } else {
      val invalidFields = ArrayList<String>()
      if (!data.containsKey("season_id") || data.getInteger("season_id") == null) {
        invalidFields.add("Season is a required field")
      }
      if (!data.containsKey("name") || data.getString("name").isNullOrBlank()) {
        invalidFields.add("Name is a required field")
      }
      if (!data.containsKey("start_date") || data.getString("start_date").isNullOrBlank()) {
        invalidFields.add("Start Date is a required field")
      }
      if (!data.containsKey("end_date") || data.getString("end_date").isNullOrBlank()) {
        invalidFields.add("End Date is a required field")
      }
      if (!data.containsKey("building_ids") || data.getJsonArray("building_ids").size() == 0) {
        invalidFields.add("One or more buildings must be selected")
      }

      if(invalidFields.size > 0){
        throw InvalidParameterException(invalidFields.joinToString(", "))
      }
    }
  }

  suspend fun validateBookingDates(data: JsonObject){
    val params = Tuple.tuple()
    params.addLocalDate(LocalDate.parse(data.getString("start_date")))

    val startDateSeason = execQuery(seasonSqlQueries.getSeasonForDates, params)

    val params2 = Tuple.tuple()
    params2.addLocalDate(LocalDate.parse(data.getString("end_date")))
    val endDateSeason = execQuery(seasonSqlQueries.getSeasonForDates, params2)

    if(startDateSeason.size() == 0 || endDateSeason.size() == 0){
      throw IllegalArgumentException("Booking dates must be within season start and end dates")
    } else {
      val seasonOne = startDateSeason.first()
      val seasonTwo = endDateSeason.first()

      if(seasonOne.getInteger("id") != seasonTwo.getInteger("id")){
        throw IllegalArgumentException("Booking dates are within different seasons")
      }
    }
  }
}
