package us.pgodfrey.sunsetlodge

import helpers.GetSession
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.RedirectConfig
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.ClassRule
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.util.*

@DisplayName("RatesTest")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingsTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  var sessionValue: String? = null
  var refreshValue: String? = null
  private var httpPort = 0

  val today = LocalDate.now()


  val bookingObject = JsonObject()
      .put("season_id", 1)
      .put("building_ids", JsonArray().add(1))
      .put("name", "Test family")
      .put("start_date", "2022-06-10")
      .put("end_date", "2022-06-17")


  val seasonObject = JsonObject()
    .put("name", "2022")
    .put("start_date", "2022-06-01")
    .put("end_date", "2022-10-08")
    .put("high_season_start_date", "2022-06-10")
    .put("high_season_end_date", "2022-09-15")
    .put("is_open", false)

  @ClassRule
  var postgreSQLContainer = PostgreSQLContainer("postgres:15")
    .withDatabaseName("sunsetlodge")
    .withUsername("sunset")
    .withPassword("abc123")


  @BeforeAll
  fun prepareSpec(vertx: Vertx, testContext: VertxTestContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      postgreSQLContainer.getPortBindings().add("5432:5432")

      val env = System.getenv()
      httpPort = env.getOrDefault("HTTP_PORT", "8081").toInt()

      //create request specification
      requestSpecification = RequestSpecBuilder()
        .addFilters(Arrays.asList(ResponseLoggingFilter(), RequestLoggingFilter()))
        .setBaseUri("http://localhost:${httpPort}/")
        .build()

      postgreSQLContainer.start()

      val flyway = Flyway.configure().dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword()).load()
      flyway.migrate()

      vertx.deployVerticle(MainVerticle(), testContext.succeeding<String> { _ ->
        val cookies = GetSession().getAuthCookies()
        sessionValue = cookies["auth-token"]
        refreshValue = cookies["refresh-token"]

        testContext.completeNow()
      })

    }
  }

  @AfterAll
  fun stopContainers(vertx: Vertx) {
    postgreSQLContainer.stop()
  }

  @Test
  @Order(0)
  @DisplayName("Not a Test: Create Season")
  fun createSeasons() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(seasonObject.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/seasons")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    seasonObject.put("id", jsonPath.getInt("data.id"))
  }

  @Test
  @Order(1)
  @DisplayName("Test Get Bookings for Existing Season")
  fun getBookings() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", 1)
      .get("/api/bookings")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(0)
  }


  @Test
  @Order(2)
  @DisplayName("Invalid Body Create Rate")
  fun invalidBodyCreateBooking() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("No request body")
  }

  @Test
  @Order(3)
  @DisplayName("Invalid Body, No Season Error")
  fun invalidBodyNoSeason() {
    val data = JsonObject()
      .put("building_ids", JsonArray().add(1))
      .put("name", "Test family")
      .put("start_date", "2022-06-10")
      .put("end_date", "2022-06-17")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Season is a required field")
  }

  @Test
  @Order(4)
  @DisplayName("Invalid Body, No Buildings Error1")
  fun invalidBodyNoBuildings1() {
    val data = JsonObject()
      .put("season_id", 1)
      .put("name", "Test family")
      .put("start_date", "2022-06-10")
      .put("end_date", "2022-06-17")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("One or more buildings must be selected")
  }

  @Test
  @Order(5)
  @DisplayName("Invalid Body, No Buildings Error2")
  fun invalidBodyNoBuildings2() {
    val data = JsonObject()
      .put("season_id", 1)
      .put("building_ids", JsonArray())
      .put("name", "Test family")
      .put("start_date", "2022-06-10")
      .put("end_date", "2022-06-17")


    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("One or more buildings must be selected")
  }

  @Test
  @Order(6)
  @DisplayName("Invalid Body, All Errors Error")
  fun invalidBodyNoBuilding() {
    val data = JsonObject()

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Season is a required field, Name is a required field, Start Date is a required field, " +
      "End Date is a required field, One or more buildings must be selected")
  }


  @Test
  @Order(7)
  @DisplayName("Invalid Dates - outside of season")
  fun invalidDatesOutsideOfSeason() {
    val data = bookingObject.copy()

    data.put("start_date", "2020-06-10")
    data.put("end_date", "2020-06-17")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Booking dates must be within season start and end dates")
  }

  @Test
  @Order(8)
  @DisplayName("Invalid Dates - different seasons")
  fun invalidDatesDifferentSeasons() {
    val data = bookingObject.copy()

    data.put("start_date", "2022-06-10")
    data.put("end_date", "2023-06-17")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Booking dates are within different seasons")
  }

  @Test
  @Order(8)
  @DisplayName("Create Booking")
  fun createBooking() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(bookingObject.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/bookings")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    bookingObject.put("id", jsonPath.getInt("data.id"))

    assertThat(jsonPath.getInt("data.id")).isEqualTo(1)
    assertThat(jsonPath.getInt("data.season_id")).isEqualTo(1)
    assertThat(jsonPath.getString("data.name")).isEqualTo("Test family")
    assertThat(jsonPath.getString("data.start_date")).isEqualTo("2022-06-10")
    assertThat(jsonPath.getString("data.end_date")).isEqualTo("2022-06-17")

  }

  @Test
  @Order(9)
  @DisplayName("Test Get Bookings after create")
  fun getBookingsAfterCreate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", 1)
      .get("/api/bookings")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)
  }

  @Test
  @Order(10)
  @DisplayName("Test Update Booking")
  fun updateRate() {
    bookingObject.put("name", "Updated Name")
    bookingObject.put("building_ids", JsonArray().add(2).add(3))

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(bookingObject.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/api/bookings/"+ bookingObject.getInteger("id"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(11)
  @DisplayName("Test Get Bookings after update")
  fun getBookingsAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", 1)
      .get("/api/bookings/")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("Updated Name")
    assertThat(jsonPath.getList<Any>("rows[0].buildings").size).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[0].buildings[0].id")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[0].buildings[1].id")).isEqualTo(3)

  }

  @Test
  @Order(12)
  @DisplayName("Test Delete Booking")
  fun deleteRate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .delete("/api/bookings/"+bookingObject.getInteger("id"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(13)
  @DisplayName("Test Get Seasons after delete")
  fun getSeasonsAfterDelete() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", 1)
      .get("/api/bookings/")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(0)
  }
}
