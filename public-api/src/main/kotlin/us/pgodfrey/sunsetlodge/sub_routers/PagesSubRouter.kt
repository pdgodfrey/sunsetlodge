package us.pgodfrey.sunsetlodge.sub_routers

import com.github.jknack.handlebars.Handlebars
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.CSRFHandler
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.helpers.HandleBarsHelperSource
import us.pgodfrey.sunsetlodge.sql.*
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters



class PagesSubRouter(vertx: Vertx, pool: Pool, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pool, jwtAuth) {


  private val pageSqlQueries = PageSqlQueries()
  private val seasonSqlQueries = SeasonSqlQueries()
  private val gallerySqlQueries = GallerySqlQueries()
  private val imageSqlQueries = ImageSqlQueries()
  private val miscSqlQueries = MiscSqlQueries()
  private var engine: HandlebarsTemplateEngine

  val dateFormat = DateTimeFormatter.ofPattern("LLLL d, yyyy");
  val dateFormatNoYear = DateTimeFormatter.ofPattern("LLLL d");
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

  var contactUsRecipientEmail = ""

  init {
    val env = System.getenv()
    contactUsRecipientEmail = env.getOrDefault("CONTACT_US_RECIPIENT", "test@recipient.com")
    currencyFormat.maximumFractionDigits = 0

    engine = HandlebarsTemplateEngine.create(vertx)

    val helperSource = HandleBarsHelperSource()
    val handlebars = engine.unwrap<Handlebars>()
    handlebars.registerHelpers(helperSource)

    router.get("/").coroutineHandler(this::handleHome)
    router.get("/rates-and-availability").coroutineHandler(this::handleRatesAndAvailability)
    router.get("/rates-and-availability/:season_name").coroutineHandler(this::handleRatesAndAvailabilityForSeason)
    router.get("/contact-us").coroutineHandler(this::handleContactUs)
    router.get("/things-to-do").coroutineHandler(this::handleThingstoDo)
    router.get("/lodge-and-cabins").coroutineHandler(this::handleLodgeAndCabins)
    router.get("/sunsets").coroutineHandler(this::handleSunsets)
    router.get("/weddings").coroutineHandler(this::handleWeddings)

    router.post("/contact-us").coroutineHandler(this::handleContactUsSubmit)
  }

  private suspend fun handleHome(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Home")
        .put("background_url", getBackgroundImageUrl())

      val openSeasons = execQuery(seasonSqlQueries.getOpenSeasons).map {
        it.toJson()
      }
      data.put("open_seasons", openSeasons)

      engine.render(data, "pages/home.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8")
            .end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }


  private suspend fun handleRatesAndAvailability(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Rates and Availability")
        .put("background_url", getBackgroundImageUrl())


      val currentSeason = execQuery(seasonSqlQueries.getCurrentSeason).first()

      val startDate = currentSeason.getLocalDate("start_date")
      val endDate = currentSeason.getLocalDate("end_date")

      logger.info("START DATE: $startDate")
      logger.info(startDate.format(dateFormat))

      data.put("start_date", startDate.format(dateFormatNoYear))
      data.put("end_date", endDate.format(dateFormat))

      val currentSeasonObj = currentSeason.toJson()

      if(currentSeasonObj.getInteger("sheet_rate") != null) {
        currentSeasonObj.put("sheet_rate", currencyFormat.format(currentSeasonObj.getInteger("sheet_rate")))
      }

      if(currentSeasonObj.getInteger("boat_package_rate") != null) {
        currentSeasonObj.put("boat_package_rate", currencyFormat.format(currentSeasonObj.getInteger("boat_package_rate")))
      }

      if(currentSeasonObj.getInteger("boat_separate_rate") != null) {
        currentSeasonObj.put("boat_separate_rate", currencyFormat.format(currentSeasonObj.getInteger("boat_separate_rate")))
      }

      data.put("current_season", currentSeasonObj)

      val nextSeasons = execQuery(seasonSqlQueries.getNextSeason)
      if(nextSeasons.size() > 0) {
        data.put("next_season", nextSeasons.first().toJson())
      }

      val highSeasonRates = execQuery(pageSqlQueries.getHighSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
        .map {
          val obj = it.toJson()

          if(obj.getInteger("high_season_rate") != null) {
            obj.put("high_season_rate", currencyFormat.format(obj.getInteger("high_season_rate")))
          }

          obj
        }

      data.put("high_current_rates", highSeasonRates)

      val lowSeasonRates = execQuery(pageSqlQueries.getLowSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
        .map {
          val obj = it.toJson()

          if(obj.getInteger("low_season_rate") != null) {
            obj.put("low_season_rate", currencyFormat.format(obj.getInteger("low_season_rate")))
          }

          obj
        }

      data.put("low_current_rates", lowSeasonRates)


      val dailyRates = execQuery(pageSqlQueries.getDailyRatesForSeason, Tuple.of(currentSeason.getInteger("id"))).map {
        val obj = it.toJson()

        if(obj.getInteger("three_night_rate") != null) {
          obj.put("three_night_rate", currencyFormat.format(obj.getInteger("three_night_rate")))
        }
        if(obj.getInteger("additional_night_rate") != null) {
          obj.put("additional_night_rate", currencyFormat.format(obj.getInteger("additional_night_rate")))
        }

        obj
      }
      data.put("daily_rates", dailyRates)

      // Retrieve active bookings
      val bookings = execQuery(pageSqlQueries.getBookingsForSeason, Tuple.of(currentSeason.getInteger("id")))

      // Retrieve building identifiers
      val buildings = execQuery(miscSqlQueries.getBuildings)

      // Prepare response for buildings and booking status
      val buildingAvailability = JsonArray()

      for (building in buildings) {
        val buildingId = building.getString("identifier")
        val buildingData = JsonObject()
          .put("name", building.getString("name"))
          .put("identifier", buildingId)

        // Generate months and dates for current season
        val months = getAvailabilityForBuilding(startDate, endDate, buildingId, bookings)

        buildingData.put("availability", months)
        buildingAvailability.add(buildingData)
      }

//      logger.info(buildingAvailability.encodePrettily())

      data.put("buildings", buildingAvailability)




      engine.render(data, "pages/rates-and-availability.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }


  private suspend fun handleRatesAndAvailabilityForSeason(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Rates and Availability")
        .put("background_url", getBackgroundImageUrl())

      val currentSeasons = execQuery(seasonSqlQueries.getSeasonByName, Tuple.of(ctx.pathParam("season_name")))

      if(currentSeasons.size() == 0) {
        ctx.response().setStatusCode(404).end()
      }

      val currentSeason = currentSeasons.first()

      val startDate = currentSeason.getLocalDate("start_date")
      val endDate = currentSeason.getLocalDate("end_date")

      logger.info("START DATE: $startDate")
      logger.info(startDate.format(dateFormat))

      data.put("start_date", startDate.format(dateFormatNoYear))
      data.put("end_date", endDate.format(dateFormat))

      val currentSeasonObj = currentSeason.toJson()

      if(currentSeasonObj.getInteger("sheet_rate") != null) {
        currentSeasonObj.put("sheet_rate", currencyFormat.format(currentSeasonObj.getInteger("sheet_rate")))
      }

      if(currentSeasonObj.getInteger("boat_package_rate") != null) {
        currentSeasonObj.put("boat_package_rate", currencyFormat.format(currentSeasonObj.getInteger("boat_package_rate")))
      }

      if(currentSeasonObj.getInteger("boat_separate_rate") != null) {
        currentSeasonObj.put("boat_separate_rate", currencyFormat.format(currentSeasonObj.getInteger("boat_separate_rate")))
      }

      data.put("current_season", currentSeasonObj)


      val highSeasonRates = execQuery(pageSqlQueries.getHighSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
        .map {
          val obj = it.toJson()

          if(obj.getInteger("high_season_rate") != null) {
            obj.put("high_season_rate", currencyFormat.format(obj.getInteger("high_season_rate")))
          }

          obj
        }

      data.put("high_current_rates", highSeasonRates)

      val lowSeasonRates = execQuery(pageSqlQueries.getLowSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
        .map {
          val obj = it.toJson()

          if(obj.getInteger("low_season_rate") != null) {
            obj.put("low_season_rate", currencyFormat.format(obj.getInteger("low_season_rate")))
          }

          obj
        }

      data.put("low_current_rates", lowSeasonRates)


      val dailyRates = execQuery(pageSqlQueries.getDailyRatesForSeason, Tuple.of(currentSeason.getInteger("id"))).map {
        val obj = it.toJson()

        if(obj.getInteger("three_night_rate") != null) {
          obj.put("three_night_rate", currencyFormat.format(obj.getInteger("three_night_rate")))
        }
        if(obj.getInteger("additional_night_rate") != null) {
          obj.put("additional_night_rate", currencyFormat.format(obj.getInteger("additional_night_rate")))
        }

        obj
      }
      data.put("daily_rates", dailyRates)

      // Retrieve active bookings
      val bookings = execQuery(pageSqlQueries.getBookingsForSeason, Tuple.of(currentSeason.getInteger("id")))

      // Retrieve building identifiers
      val buildings = execQuery(miscSqlQueries.getBuildings)

      // Prepare response for buildings and booking status
      val buildingAvailability = JsonArray()

      for (building in buildings) {
        val buildingId = building.getString("identifier")
        val buildingData = JsonObject()
          .put("name", building.getString("name"))
          .put("identifier", buildingId)

        // Generate months and dates for current season
        val months = getAvailabilityForBuilding(startDate, endDate, buildingId, bookings)

        buildingData.put("availability", months)
        buildingAvailability.add(buildingData)
      }

//      logger.info(buildingAvailability.encodePrettily())

      data.put("buildings", buildingAvailability)




      engine.render(data, "pages/rates-and-availability.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun handleContactUs(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Contact Us")
        .put("background_url", getBackgroundImageUrl())
        .put("csrf_token", ctx.get("X-XSRF-TOKEN"))


      engine.render(data, "pages/contact-us.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun handleContactUsSubmit(ctx: RoutingContext) {
    try {
      val params = ctx.request().formAttributes()


      val hostHeader = ctx.request().getHeader("Host")

      val emailObj = JsonObject()
        .put("recipient_email", contactUsRecipientEmail)
        .put("subject", "Sunset Lodge Website Inquiry")
        .put("domain", hostHeader)
        .put("name", params.get("name"))
        .put("email", params.get("email"))
        .put("phone", params.get("phone"))
        .put("reason_for_contact", params.get("reason_for_contact"))
        .put("message", params.get("message").replace("\n", "<br/>"))
        .put("template", "contact.hbs")

      val backgroundUrl = getBackgroundImageUrl()

      vertx.eventBus().request<Any>("email.send", emailObj) {

        val data: JsonObject = JsonObject()
          .put("title", "Contact Us")
          .put("background_url", backgroundUrl)

        engine.render(data, "pages/contact-us-thanks.hbs") { res ->
          if (res.succeeded()) {
            ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
          } else {
            ctx.fail(res.cause())
          }
        }
      }

    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun handleThingstoDo(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Things to Do")
        .put("background_url", getBackgroundImageUrl())

      engine.render(data, "pages/things-to-do.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8")
            .end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun handleLodgeAndCabins(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Lodges and Cabins")
        .put("background_url", getBackgroundImageUrl())

      val galleryCategory = execQuery(gallerySqlQueries.getGalleryCategoryByName, Tuple.of("The Lodge And Cabins")).first()

      data.put("gallery_category_description", galleryCategory.getString("description").replace("\n", "<br/>"))

      val galleriesData = getGalleryDataForGalleryCategory(galleryCategory)

      logger.info("DATA")
      logger.info("${galleriesData}")
      data.put("galleries", galleriesData)

      engine.render(data, "pages/lodge-and-cabins.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun handleSunsets(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Sunsets")
        .put("background_url", getBackgroundImageUrl())

      val galleryCategory = execQuery(gallerySqlQueries.getGalleryCategoryByName, Tuple.of("Sunsets")).first()

      data.put("gallery_category_description", galleryCategory.getString("description").replace("\n", "<br/>"))

      val galleriesData = getGalleryDataForGalleryCategory(galleryCategory)
      data.put("gallery", galleriesData.getJsonObject(0))

      engine.render(data, "pages/sunsets.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun handleWeddings(ctx: RoutingContext) {
    try {
      val data: JsonObject = JsonObject()
        .put("title", "Weddings")
        .put("background_url", getBackgroundImageUrl())

      val galleryCategory = execQuery(gallerySqlQueries.getGalleryCategoryByName, Tuple.of("Weddings")).first()
      data.put("gallery_category_description",
        galleryCategory.getString("description")
          .replace("\n", "<br/>")
          .replaceFirst("Sunset Cabins", "<a href=\"http://www.sunsetcabinsmaine.com\" target=\"_blank\">Sunset Cabins</a>")
      )

      val galleriesData = getGalleryDataForGalleryCategory(galleryCategory)

      data.put("galleries", galleriesData)

      engine.render(data, "pages/weddings.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private suspend fun selectBackgroundImage(): Row {

    val galleryCategory = execQuery(gallerySqlQueries.getGalleryCategoryByName, Tuple.of("Backgrounds")).first()
    val gallery = execQuery(gallerySqlQueries.getGalleryByIdentifierAndCategoryId, Tuple.of("Backgrounds", galleryCategory.getInteger("id"))).first()

    val backgroundImage = execQuery(imageSqlQueries.getRandomImageForGallery, Tuple.of(gallery.getInteger("id"))).first()

    return backgroundImage
  }

  private suspend fun getBackgroundImageUrl(): String {
    val backgroundImage = selectBackgroundImage()
    return getLargeUrl(backgroundImage)
  }

  private fun getImageUrl(image: Row): String {
    return "/gallery-images/${image.getInteger("id")}/${image.getString("filename")}"
  }

  private fun getThumbnailUrl(image: Row): String {
    val filename = image.getString("filename").substringBeforeLast(".")

    return "/gallery-images/${image.getInteger("id")}/${filename}_thumb.png"
  }

  private fun getLargeUrl(image: Row): String {
    val filename = image.getString("filename").substringBeforeLast(".")

    return "/gallery-images/${image.getInteger("id")}/${filename}_large.png"
  }

  private suspend fun getGalleryDataForGalleryCategory(galleryCategory: Row): JsonArray {

    val galleries = execQuery(gallerySqlQueries.getGalleriesForCategory, Tuple.of(galleryCategory.getInteger("id")))
    val galleriesData = JsonArray()
    galleries.forEach { gallery ->
      val galleryData = JsonObject()
      galleryData.put("name", gallery.getString("identifier"))
      galleryData.put("anchor", gallery
        .getString("identifier")
        .lowercase().replace(" ", "-")
        .replace("'", "")
        .replace("/", ""))
      if(gallery.getString("description") != null){
        galleryData.put("description", gallery.getString("description").replace("\n", "<br/>").trim())
      }

      val images = execQuery(imageSqlQueries.getImagesForGallery, Tuple.of(gallery.getInteger("id")))

      val imagesData = images.map { image ->
        val obj = JsonObject()
        obj.put("large_url", getLargeUrl(image))
        obj
      }

      val thumbsData = images.map { image ->
        val obj = JsonObject()
        obj.put("thumb_url", getThumbnailUrl(image))
        obj
      }.chunked(6).mapIndexed { index, jsonObjects ->
        val obj = JsonObject()
          .put("first", index == 0)
          .put("thumbs", jsonObjects)

        obj
      }

      galleryData.put("images", imagesData)
      galleryData.put("thumbs", thumbsData)

      galleriesData.add(galleryData)
    }

    return galleriesData
  }

  private fun getAvailabilityForBuilding(
    startDate: LocalDate,
    endDate: LocalDate,
    buildingId: String,
    bookings: RowSet<Row>
  ): JsonArray {
    val months = JsonArray()

    var current = startDate.withDayOfMonth(1) // Start from the first day of the month
    val end = endDate.withDayOfMonth(1) // End at the first day of the endDate's month

    while (!current.isAfter(end)) {
      val monthStart = current.withDayOfMonth(1) // First day of the current month
      val monthEnd = current.with(TemporalAdjusters.lastDayOfMonth()) // Last day of the current month

      val startWithSunday = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
      val endWithSaturday = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

      // Create the month object
      val monthObj = JsonObject()
      monthObj.put("name", "${current.month.name}-${current.year}")

      // Generate availability for each date and group into weeks
      val weeks = JsonArray()
      var week = JsonArray() // Collect 7 days into a week
      var date = startWithSunday

      while (!date.isAfter(endWithSaturday)) {
        val isBooked = isDateBooked(buildingId, date, bookings)
        val isClosed = date.isBefore(startDate) || date.isAfter(endDate) // Indicates if the date is outside season range
        val outsideMonth = date.isBefore(monthStart) || date.isAfter(monthEnd) // Indicates if the date is outside the current month

        // Create the date object
        val dateObj = JsonObject()
          .put("date", date.toString())
          .put("dayOfMonth", date.dayOfMonth)
          .put("isBooked", isBooked)
          .put("isClosed", isClosed)
          .put("isAvailable", !(isBooked || isClosed || outsideMonth))
          .put("outsideMonth", outsideMonth)

        // Add the date to the current week
        week.add(dateObj)

        // If the week is complete, add it to weeks and start a new one
        if (week.size() == 7) {
          weeks.add(week)
          week = JsonArray()
        }

        // Move to the next day
        date = date.plusDays(1)
      }

      // Add the final week if it has any remaining dates
      if (week.size() > 0) {
        weeks.add(week)
      }

      monthObj.put("weeks", weeks)
      months.add(monthObj)

      current = current.plusMonths(1)
    }

    return months
  }


  private fun isDateBooked(buildingId: String, date: LocalDate, bookings: RowSet<Row>): Boolean {
    for (booking in bookings) {
      // Check if the booking includes the building and overlaps with the date
      val buildingIds = booking.getArrayOfStrings("buildings")
      val bookingStart = booking.getLocalDate("start_date")
      val bookingEnd = booking.getLocalDate("end_date")

      if ((buildingIds.contains(buildingId) || buildingIds.contains("all")) &&
          (date.isEqual(bookingStart) || date.isEqual(bookingEnd) ||
            (date.isAfter(bookingStart) && date.isBefore(bookingEnd))
            )
        ) {
        return true
      }
    }
    return false
  }

}
