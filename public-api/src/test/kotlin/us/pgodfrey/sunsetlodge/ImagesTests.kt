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
import java.io.File
import java.time.LocalDate
import java.util.*

@DisplayName("RatesTest")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImagesTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  var sessionValue: String? = null
  var refreshValue: String? = null
  private var httpPort = 0

  val today = LocalDate.now()

  val galleryOneImages = listOf(
    "src/test/resources/sunset-01.jpg",
    "src/test/resources/sunset-02.jpg",
    "src/test/resources/sunset-03.jpg"
  )

  val galleryTwoImages =listOf(
    "src/test/resources/sunset-04.jpg",
    "src/test/resources/sunset-05.jpg"
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
        GetSession().setPassword()
        val response = GetSession().loginResponse()
        val cookies = response.cookies()
        val jsonPath = response.jsonPath()
        sessionValue = jsonPath.getString("token")
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
  @DisplayName("Create Images")
  fun createImages() {
    galleryOneImages.forEach { filePath ->
      val jsonPath = RestAssured.given(requestSpecification)
        .given()
        .multiPart("gallery_id", 1)
        .multiPart("file", File(filePath))
        .header("Authorization", "Bearer ${sessionValue}")
        .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
        .post("/api/images")
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath()

      assertThat(jsonPath.getBoolean("success")).isTrue()
    }
    galleryTwoImages.forEach { filePath ->
      val jsonPath = RestAssured.given(requestSpecification)
        .given()
        .multiPart("gallery_id", 2)
        .multiPart("file", File(filePath))
        .header("Authorization", "Bearer ${sessionValue}")
        .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
        .post("/api/images")
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath()

      assertThat(jsonPath.getBoolean("success")).isTrue()
    }
  }


  @Test
  @Order(2)
  @DisplayName("Test Get Images for Gallery One")
  fun getImagesForGalleryOne() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_id", 1)
      .get("/api/images")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(3)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[0].gallery_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[0].order_by")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].url")).isEqualTo("/gallery-images/1/sunset-01.jpg")
    assertThat(jsonPath.getString("rows[0].thumbnail_url")).isEqualTo("/gallery-images/1/sunset-01_thumb.png")

    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[1].gallery_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[1].order_by")).isEqualTo(2)
    assertThat(jsonPath.getString("rows[1].url")).isEqualTo("/gallery-images/2/sunset-02.jpg")
    assertThat(jsonPath.getString("rows[1].thumbnail_url")).isEqualTo("/gallery-images/2/sunset-02_thumb.png")

    assertThat(jsonPath.getInt("rows[2].id")).isEqualTo(3)
    assertThat(jsonPath.getInt("rows[2].gallery_id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[2].order_by")).isEqualTo(3)
    assertThat(jsonPath.getString("rows[2].url")).isEqualTo("/gallery-images/3/sunset-03.jpg")
    assertThat(jsonPath.getString("rows[2].thumbnail_url")).isEqualTo("/gallery-images/3/sunset-03_thumb.png")
  }

  @Test
  @Order(3)
  @DisplayName("Test Get Images for Gallery Two")
  fun getImagesForGalleryTwo() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_id", 2)
      .get("/api/images")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(4)
    assertThat(jsonPath.getInt("rows[0].gallery_id")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[0].order_by")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].url")).isEqualTo("/gallery-images/4/sunset-04.jpg")
    assertThat(jsonPath.getString("rows[0].thumbnail_url")).isEqualTo("/gallery-images/4/sunset-04_thumb.png")

    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(5)
    assertThat(jsonPath.getInt("rows[1].gallery_id")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[1].order_by")).isEqualTo(2)
    assertThat(jsonPath.getString("rows[1].url")).isEqualTo("/gallery-images/5/sunset-05.jpg")
    assertThat(jsonPath.getString("rows[1].thumbnail_url")).isEqualTo("/gallery-images/5/sunset-05_thumb.png")

  }

  @Test
  @Order(3)
  @DisplayName("Invalid, No files")
  fun invalidNoFiles() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .multiPart("gallery_id", 2)
      .post("/api/images")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Must have one file upload")
  }

  @Test
  @Order(4)
  @DisplayName("Invalid, No Gallery Id")
  fun invalidNoGalleryId() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .multiPart("file", File(galleryOneImages.get(0)))
      .post("/api/images")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Gallery Id is required")
  }

  @Test
  @Order(5)
  @DisplayName("Test Update Order Bys")
  fun updateOrderBys() {
    val data = JsonObject()
      .put("image_ids", JsonArray().add(3).add(1).add(2))

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/images/update-order")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(6)
  @DisplayName("Test Get images after update")
  fun getRatesAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_id", 1)
      .get("/api/images/")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(3)
    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[2].id")).isEqualTo(2)

  }

  @Test
  @Order(7)
  @DisplayName("Test Delete Image")
  fun deleteImage() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .delete("/api/images/1")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(8)
  @DisplayName("Test Get Images after delete")
  fun getImagesAfterDelete() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .queryParam("gallery_id", 1)
      .get("/api/images/")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(3)
    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(2)
  }


  @Test
  @Order(9)
  @DisplayName("Test Get existing image")
  fun getExistingImage() {
    RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/gallery-images/2/sunset-02.jpg")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
  }

  @Test
  @Order(10)
  @DisplayName("Test Get existing thumbnail image")
  fun getExistingThumbnailImage() {
    RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/gallery-images/2/sunset-02_thumb.png")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
  }

  @Test
  @Order(11)
  @DisplayName("Test Get non-existising image")
  fun getNonExistingImage() {
    RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/gallery-images/2/sunset-02.png")
      .then()
      .assertThat()
      .statusCode(404)
      .extract()
  }
}
