package us.pgodfrey.sunsetlodge.sub_routers

import com.github.jknack.handlebars.Handlebars
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import us.pgodfrey.sunsetlodge.BaseSubRouter
import us.pgodfrey.sunsetlodge.helpers.HandleBarsHelperSource
import us.pgodfrey.sunsetlodge.sql.GallerySqlQueries
import us.pgodfrey.sunsetlodge.sql.ImageSqlQueries
import us.pgodfrey.sunsetlodge.sql.PageSqlQueries
import us.pgodfrey.sunsetlodge.sql.SeasonSqlQueries


class PagesSubRouter(vertx: Vertx, pool: Pool, jwtAuth: JWTAuth) : BaseSubRouter(vertx, pool, jwtAuth) {


  private val pageSqlQueries = PageSqlQueries()
  private val seasonSqlQueries = SeasonSqlQueries()
  private val gallerySqlQueries = GallerySqlQueries()
  private val imageSqlQueries = ImageSqlQueries()
  private var engine: HandlebarsTemplateEngine

  init {

    engine = HandlebarsTemplateEngine.create(vertx)

    val helperSource = HandleBarsHelperSource()
    val handlebars = engine.unwrap<Handlebars>()
    handlebars.registerHelpers(helperSource)

    router.get("/").coroutineHandler(this::handleHome)
    router.get("/rates-and-availability").coroutineHandler(this::handleRatesAndAvailability)
    router.get("/contact-us").coroutineHandler(this::handleContactUs)
    router.get("/things-to-do").coroutineHandler(this::handleThingstoDo)
    router.get("/lodge-and-cabins").coroutineHandler(this::handleLodgeAndCabins)
    router.get("/sunsets").coroutineHandler(this::handleSunsets)
    router.get("/weddings").coroutineHandler(this::handleWeddings)
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
          ctx.response().end(res.result())
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

      val currentSeason = execQuery(seasonSqlQueries.getCurrentSeason).first()
      data.put("current_season", currentSeason.toJson())

      val highSeasonRates = execQuery(pageSqlQueries.getHighSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
        .map {
          it.toJson()
        }

      data.put("high_current_rates", highSeasonRates)

      val lowSeasonRates = execQuery(pageSqlQueries.getLowSeasonRatesForSeason, Tuple.of(currentSeason.getInteger("id")))
        .map {
          it.toJson()
        }

      data.put("low_current_rates", lowSeasonRates)

      engine.render(data, "pages/rates-and-availability.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
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

      engine.render(data, "pages/contact-us.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
        } else {
          ctx.fail(res.cause())
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

      engine.render(data, "pages/things-to-do.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
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
      data.put("gallery_category_description", galleryCategory.getString("description"))

      val galleries = execQuery(gallerySqlQueries.getGalleriesForCategory, Tuple.of(galleryCategory.getInteger("id")))

//        val galleryData = JsonObject()
      galleries.forEach { gallery ->

      }


      engine.render(data, "pages/lodge-and-cabins.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
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

      engine.render(data, "pages/sunsets.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
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

      engine.render(data, "pages/weddings.hbs") { res ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
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
    return getImageUrl(backgroundImage)
  }

  private fun getImageUrl(image: Row): String {
    return "/gallery-images/${image.getInteger("id")}/${image.getString("filename")}"
  }

  private fun getThumbnailUrl(image: Row): String {
    val filename = image.getString("filename").substringBeforeLast(".")

    return "/gallery-images/${image.getInteger("id")}/${filename}_thumb.png"
  }
}
