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
import java.util.*

@DisplayName("SimpleObjectsTests")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GalleriesTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  var sessionValue: String? = null
  var refreshValue: String? = null
  private var httpPort = 0

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
        GetSession().setPassword()
        val response = GetSession().loginResponse()
        val cookies = response.cookies()
        val jsonPath = response.jsonPath()
        sessionValue = cookies["auth-token"]
        refreshValue = jsonPath.getString("refresh_token")

        testContext.completeNow()
      })
    }
  }

  @AfterAll
  fun stopContainers(vertx: Vertx) {
    postgreSQLContainer.stop()
  }

  @Test
  @Order(1)
  @DisplayName("Test Get Gallery Categories")
  fun getGalleryCategories() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/galleries/gallery-categories")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(4)

    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("The Lodge And Cabins")
    assertThat(jsonPath.getString("rows[0].description")).isNull()
    assertThat(jsonPath.getString("rows[1].name")).isEqualTo("Sunsets")
    assertThat(jsonPath.getString("rows[1].description")).isNull()
    assertThat(jsonPath.getString("rows[2].name")).isEqualTo("Weddings")
    assertThat(jsonPath.getString("rows[2].description")).isNull()
    assertThat(jsonPath.getString("rows[3].name")).isEqualTo("Backgrounds")
    assertThat(jsonPath.getString("rows[3].description")).isNull()
  }



  @Test
  @Order(2)
  @DisplayName("Test Update Gallery Category Description")
  fun updateGalleryCategoryDescription() {
    val data = JsonObject()
      .put("id", 1)
      .put("description", "Testing")

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/api/galleries/gallery-categories/"+ data.getInteger("id"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(3)
  @DisplayName("Test Get Gallery Categories After Update")
  fun getGalleryCategoriesAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/galleries/gallery-categories")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(4)

    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("The Lodge And Cabins")
    assertThat(jsonPath.getString("rows[0].description")).isEqualTo("Testing")
    assertThat(jsonPath.getString("rows[1].name")).isEqualTo("Sunsets")
    assertThat(jsonPath.getString("rows[1].description")).isNull()
    assertThat(jsonPath.getString("rows[2].name")).isEqualTo("Weddings")
    assertThat(jsonPath.getString("rows[2].description")).isNull()
    assertThat(jsonPath.getString("rows[3].name")).isEqualTo("Backgrounds")
    assertThat(jsonPath.getString("rows[3].description")).isNull()
  }

  @Test
  @Order(4)
  @DisplayName("Test Get Galleries For Category 1")
  fun getGalleriesForOne() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_category_id", 1)
      .get("/api/galleries")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(6)

    assertThat(jsonPath.getString("rows[0].identifier")).isEqualTo("The Lodge")
    assertThat(jsonPath.getString("rows[0].description")).isNull()
    assertThat(jsonPath.getString("rows[1].identifier")).isEqualTo("Lakeside Cabin")
    assertThat(jsonPath.getString("rows[1].description")).isNull()
    assertThat(jsonPath.getString("rows[2].identifier")).isEqualTo("Judy's Cabin")
    assertThat(jsonPath.getString("rows[2].description")).isNull()
    assertThat(jsonPath.getString("rows[3].identifier")).isEqualTo("Tree House Cabin")
    assertThat(jsonPath.getString("rows[3].description")).isNull()
    assertThat(jsonPath.getString("rows[4].identifier")).isEqualTo("Bird's Nest Cabin")
    assertThat(jsonPath.getString("rows[4].description")).isNull()
    assertThat(jsonPath.getString("rows[5].identifier")).isEqualTo("The Grounds")
    assertThat(jsonPath.getString("rows[5].description")).isNull()

  }

  @Test
  @Order(5)
  @DisplayName("Test Get Galleries For Category 2")
  fun getGalleriesForTwo() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_category_id", 2)
      .get("/api/galleries")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)

    assertThat(jsonPath.getString("rows[0].identifier")).isEqualTo("Sunsets")
    assertThat(jsonPath.getString("rows[0].description")).isNull()

  }


  @Test
  @Order(6)
  @DisplayName("Test Get Galleries For Category 3")
  fun getGalleriesForThree() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_category_id", 3)
      .get("/api/galleries")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)

    assertThat(jsonPath.getString("rows[0].identifier")).isEqualTo("Godfrey/Kahn")
    assertThat(jsonPath.getString("rows[0].description")).isNull()

    assertThat(jsonPath.getString("rows[1].identifier")).isEqualTo("Powers/Fuqua")
    assertThat(jsonPath.getString("rows[1].description")).isNull()

  }


  @Test
  @Order(7)
  @DisplayName("Test Get Galleries For Category 4")
  fun getGalleriesForFour() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_category_id", 4)
      .get("/api/galleries")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)

    assertThat(jsonPath.getString("rows[0].identifier")).isEqualTo("Backgrounds")
    assertThat(jsonPath.getString("rows[0].description")).isNull()

  }
  @Test
  @Order(8)
  @DisplayName("Test Update Gallery Description")
  fun updateGalleryDescription() {
    val data = JsonObject()
      .put("id", 10)
      .put("description", "Testing")

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/api/galleries/"+ data.getInteger("id"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(9)
  @DisplayName("Test Get Galleries For Category 4 After Update")
  fun getGalleriesForFourAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .cookie("auth-token", sessionValue)
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_category_id", 4)
      .get("/api/galleries")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)

    assertThat(jsonPath.getString("rows[0].identifier")).isEqualTo("Backgrounds")
    assertThat(jsonPath.getString("rows[0].description")).isEqualTo("Testing")

  }
}
