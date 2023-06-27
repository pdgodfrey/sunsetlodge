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
class RatesTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  var sessionValue: String? = null
  var refreshValue: String? = null
  private var httpPort = 0

  val today = LocalDate.now()

  val seasonObjects = listOf<JsonObject>(
    JsonObject()
      .put("name", "${today.year+1}")
      .put("start_date", "${today.year+1}-06-01")
      .put("end_date", "${today.year+1}-10-08")
      .put("high_season_start_date", "${today.year+1}-06-10")
      .put("high_season_end_date", "${today.year+1}-09-15")
      .put("is_open", true),
    JsonObject()
      .put("name", "${today.year+2}")
      .put("start_date", "${today.year+2}-06-01")
      .put("end_date", "${today.year+2}-10-08")
      .put("high_season_start_date", "${today.year+2}-06-10")
      .put("high_season_end_date", "${today.year+2}-09-15")
      .put("is_open", false)
  )


  val rateObjects = listOf<JsonObject>(
    JsonObject()
      .put("season_id", null)
      .put("building_id", 1)
      .put("high_season_rate", 5000)
      .put("low_season_rate", 400),
    JsonObject()
      .put("season_id", null)
      .put("building_id", 2)
      .put("high_season_rate", 1200)
      .put("low_season_rate", null)
  )

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
  @DisplayName("Not a Test: Create Seasons")
  fun createSeasons() {
    seasonObjects.forEach { data ->
      val jsonPath = RestAssured.given(requestSpecification)
        .given()
        .contentType(ContentType.JSON)
        .body(data.encode())
        .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
        .post("/api/seasons")
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath()

      assertThat(jsonPath.getBoolean("success")).isTrue()

      data.put("id", jsonPath.getInt("data.id"))
    }

    assertThat(seasonObjects.get(0).getInteger("id")).isEqualTo(2)
    rateObjects.forEach {
      it.put("season_id", seasonObjects.get(0).getInteger("id"))
    }

    assertThat(seasonObjects.get(1).getInteger("id")).isEqualTo(3)
  }

  @Test
  @Order(1)
  @DisplayName("Test Get Rates for Existing Season")
  fun getRates() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", 1)
      .get("/api/rates")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(6)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[0].season_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[0].building_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[0].high_season_rate")).isEqualTo(8100)
    assertThat(jsonPath.getInt("rows[0].low_season_rate")).isEqualTo(7595)
    assertThat(jsonPath.getString("rows[0].building_name")).isEqualTo("All Buildings")
    assertThat(jsonPath.getString("rows[0].season_name")).isEqualTo("2023")

    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[1].season_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[1].building_id")).isEqualTo(2)
    assertThat(jsonPath.get<Any?>("rows[1].high_season_rate")).isNull()
    assertThat(jsonPath.getInt("rows[1].low_season_rate")).isEqualTo(4100)
    assertThat(jsonPath.getString("rows[1].building_name")).isEqualTo("The Lodge")
    assertThat(jsonPath.getString("rows[1].season_name")).isEqualTo("2023")

    assertThat(jsonPath.getInt("rows[2].id")).isEqualTo(3)
    assertThat(jsonPath.getInt("rows[2].season_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[2].building_id")).isEqualTo(3)
    assertThat(jsonPath.get<Any?>("rows[2].high_season_rate")).isNull()
    assertThat(jsonPath.getInt("rows[2].low_season_rate")).isEqualTo(1560)
    assertThat(jsonPath.getString("rows[2].building_name")).isEqualTo("Lakeside Cabin")
    assertThat(jsonPath.getString("rows[2].season_name")).isEqualTo("2023")

    assertThat(jsonPath.getInt("rows[3].id")).isEqualTo(4)
    assertThat(jsonPath.getInt("rows[3].season_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[3].building_id")).isEqualTo(4)
    assertThat(jsonPath.get<Any?>("rows[3].high_season_rate")).isNull()
    assertThat(jsonPath.getInt("rows[3].low_season_rate")).isEqualTo(1100)
    assertThat(jsonPath.getString("rows[3].building_name")).isEqualTo("Treehouse Cabin")
    assertThat(jsonPath.getString("rows[3].season_name")).isEqualTo("2023")

    assertThat(jsonPath.getInt("rows[4].id")).isEqualTo(5)
    assertThat(jsonPath.getInt("rows[4].season_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[4].building_id")).isEqualTo(5)
    assertThat(jsonPath.get<Any?>("rows[4].high_season_rate")).isNull()
    assertThat(jsonPath.getInt("rows[4].low_season_rate")).isEqualTo(1075)
    assertThat(jsonPath.getString("rows[4].building_name")).isEqualTo("Bird's Nest Cabin")
    assertThat(jsonPath.getString("rows[4].season_name")).isEqualTo("2023")

    assertThat(jsonPath.getInt("rows[5].id")).isEqualTo(6)
    assertThat(jsonPath.getInt("rows[5].season_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[5].building_id")).isEqualTo(6)
    assertThat(jsonPath.get<Any?>("rows[5].high_season_rate")).isNull()
    assertThat(jsonPath.getInt("rows[5].low_season_rate")).isEqualTo(1530)
    assertThat(jsonPath.getString("rows[5].building_name")).isEqualTo("Judy's Cabin")
    assertThat(jsonPath.getString("rows[5].season_name")).isEqualTo("2023")
  }


  @Test
  @Order(2)
  @DisplayName("Invalid Body Create Rate")
  fun invalidBodyCreateSeason() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/rates")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("No request body")
  }

  @Test
  @Order(4)
  @DisplayName("Create Rate - already exists")
  fun createRateAlreadyExists() {
    val data = JsonObject()
      .put("season_id", 1)
      .put("building_id", 1)
      .put("high_season_rate", 5000)
      .put("low_season_rate", 400)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/rates")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Rate already exists for this building and season")
  }

  @Test
  @Order(5)
  @DisplayName("Invalid Body, No Season Error")
  fun invalidBodyNoSeason() {
    val data = JsonObject()
      .put("building_id", 1)
      .put("high_season_rate", 5000)
      .put("low_season_rate", 400)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/rates")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Season is a required field")
  }

  @Test
  @Order(6)
  @DisplayName("Invalid Body, No Building Error")
  fun invalidBodyNoBuilding() {
    val data = JsonObject()
      .put("season_id", 1)
      .put("high_season_rate", 5000)
      .put("low_season_rate", 400)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/rates")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Building is a required field")
  }

  @Test
  @Order(7)
  @DisplayName("Non existant building")
  fun nonExistantBuilding() {
    val data = JsonObject()
      .put("building_id", 37)
      .put("season_id", 1)
      .put("high_season_rate", 5000)
      .put("low_season_rate", 400)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/rates")
      .then()
      .assertThat()
      .statusCode(500)
      .extract()
      .asString()

    assertThat(response).contains("ERROR: insert or update on table \"rates\" violates foreign key constraint \"fk_building\"")
  }

  @Test
  @Order(8)
  @DisplayName("Non existant season")
  fun nonExistantSeason() {
    val data = JsonObject()
      .put("building_id", 1)
      .put("season_id", 122)
      .put("high_season_rate", 5000)
      .put("low_season_rate", 400)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/rates")
      .then()
      .assertThat()
      .statusCode(500)
      .extract()
      .asString()

    assertThat(response).contains("ERROR: insert or update on table \"rates\" violates foreign key constraint \"fk_season\"")
  }



  @Test
  @Order(9)
  @DisplayName("Create Rates")
  fun createRates() {
    rateObjects.forEach { data ->
      val jsonPath = RestAssured.given(requestSpecification)
        .given()
        .contentType(ContentType.JSON)
        .body(data.encode())
        .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
        .post("/api/rates")
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath()

      assertThat(jsonPath.getBoolean("success")).isTrue()
      data.put("id", jsonPath.getInt("data.id"))
    }

    assertThat(rateObjects.get(0).getInteger("id")).isEqualTo(9)
    assertThat(rateObjects.get(1).getInteger("id")).isEqualTo(10)

  }

  @Test
  @Order(10)
  @DisplayName("Test Get Rates after create")
  fun getRatesAfterCreate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", seasonObjects.get(0).getInteger("id"))
      .get("/api/rates")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)
  }

  @Test
  @Order(11)
  @DisplayName("Test Update Rate")
  fun updateRate() {
    val data = rateObjects.get(1)
    data.put("low_season_rate", 100)

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/api/rates/"+ data.getInteger("id"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(12)
  @DisplayName("Test Get Rates after update")
  fun getRatesAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", seasonObjects.get(0).getInteger("id"))
      .get("/api/rates/")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getInt("rows[1].low_season_rate")).isEqualTo(1200)

  }

  @Test
  @Order(13)
  @DisplayName("Test Delete Rate")
  fun deleteRate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .delete("/api/rates/"+rateObjects.get(0).getInteger("id"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(14)
  @DisplayName("Test Get Seasons after delete")
  fun getSeasonsAfterDelete() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("season_id", seasonObjects.get(0).getInteger("id"))
      .get("/api/rates/")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)
  }
}
