package us.pgodfrey.sunsetlodge

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
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.`as`
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
import kotlin.collections.LinkedHashMap

@DisplayName("SeasonsTest")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeasonsTest {
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



      if(httpPort == 8081) {
        postgreSQLContainer.start()

        val flyway = Flyway.configure().dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword()).load()
        flyway.migrate()

        vertx.deployVerticle(MainVerticle(), testContext.succeeding<String> { _ ->
          testContext.completeNow()
        })
      } else {
        testContext.completeNow()
      }
    }
  }

  @AfterAll
  fun stopContainers(vertx: Vertx) {
    postgreSQLContainer.stop()
  }

  @Test
  @Order(1)
  @DisplayName("Test Get Seasons")
  fun getSeasons() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/seasons")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("2023")
    assertThat(jsonPath.getString("rows[0].start_date")).isEqualTo("2023-06-01")
    assertThat(jsonPath.getString("rows[0].end_date")).isEqualTo("2023-10-08")
    assertThat(jsonPath.getString("rows[0].high_season_start_date")).isEqualTo("2023-06-10")
    assertThat(jsonPath.getString("rows[0].high_season_end_date")).isEqualTo("2023-09-15")
    assertThat(jsonPath.getBoolean("rows[0].is_open")).isFalse()
  }

  @Test
  @Order(2)
  @DisplayName("Test Create Seasons")
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
    assertThat(seasonObjects.get(1).getInteger("id")).isEqualTo(3)
  }


  @Test
  @Order(3)
  @DisplayName("Invalid Body Create Season")
  fun invalidBodyCreateSeason() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/seasons")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("No request body")
  }

  @Test
  @Order(4)
  @DisplayName("Invalid Body, No Name Error")
  fun invalidBodyNoName() {
    val data = JsonObject()
      .put("start_date", "2020-06-01")
      .put("end_date", "2020-10-01")
      .put("high_season_start_date", "2020-06-08")
      .put("high_season_end_date", "2020-09-15")
      .put("is_open", false)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/seasons")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Name is a required field")
  }

  @Test
  @Order(5)
  @DisplayName("Invalid Body, All But Name Error")
  fun invalidBodyAllButName() {
    val data = JsonObject()
      .put("name", "2020")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/seasons")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Start Date is a required field, End Date is a required field, " +
      "High Season Start Date is a required field, High Season End Date is a required field, " +
      "Is Open is a required field")
  }

  @Test
  @Order(6)
  @DisplayName("Test Get Seasons after create")
  fun getSeasonsAfterCreate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/seasons")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(3)

    var currentCount = 0
    val seasons = jsonPath.getList<LinkedHashMap<String, Any>>("rows")

    seasons.forEach {
      if( it.get("is_current") as Boolean){
        currentCount += 1
      }
    }

    assertThat(currentCount).isEqualTo(1)
  }

  @Test
  @Order(7)
  @DisplayName("Get Current Season")
  fun getCurrentSeason() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/seasons/current")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    val season = jsonPath.get("season") as LinkedHashMap<String, Any>
    assertThat(season).isNotNull
  }

  @Test
  @Order(8)
  @DisplayName("Test Update Season")
  fun updateSeason() {
    val data = JsonObject()
      .put("id", 1)
      .put("name", "2023")
      .put("start_date", "2023-06-01")
      .put("end_date", "2023-10-01")
      .put("high_season_start_date", "2023-06-08")
      .put("high_season_end_date", "2023-09-15")
      .put("is_open", true)

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/api/seasons/1")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(9)
  @DisplayName("Test Get Season after update")
  fun getSeasonAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/seasons/1")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getInt("season.id")).isEqualTo(1)
    assertThat(jsonPath.getString("season.name")).isEqualTo("2023")
    assertThat(jsonPath.getString("season.start_date")).isEqualTo("2023-06-01")
    assertThat(jsonPath.getString("season.end_date")).isEqualTo("2023-10-01")
    assertThat(jsonPath.getString("season.high_season_start_date")).isEqualTo("2023-06-08")
    assertThat(jsonPath.getString("season.high_season_end_date")).isEqualTo("2023-09-15")
    assertThat(jsonPath.getBoolean("season.is_open")).isTrue()
  }

  @Test
  @Order(10)
  @DisplayName("Test Delete Season")
  fun deleteSeason() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .delete("/api/seasons/2")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(11)
  @DisplayName("Test Get Seasons after delete")
  fun getSeasonsAfterDelete() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/seasons")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)

    var currentCount = 0
    val seasons = jsonPath.getList<LinkedHashMap<String, Any>>("rows")

    seasons.forEach {
      if( it.get("is_current") as Boolean){
        currentCount += 1
      }
    }

    assertThat(currentCount).isEqualTo(1)
  }
}
