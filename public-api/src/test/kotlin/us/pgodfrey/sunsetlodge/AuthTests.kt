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
import io.vertx.ext.web.client.WebClient
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

@DisplayName("AuthTests")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  private var emailRequestSpecification: RequestSpecification? = null
  var sessionValue: String? = null
  var refreshValue: String? = null
  private var httpPort = 0


  val today = LocalDate.now()


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


      emailRequestSpecification = RequestSpecBuilder()
        .addFilters(Arrays.asList(ResponseLoggingFilter(), RequestLoggingFilter()))
        .setBaseUri("http://localhost:8025/")
        .build()

      RestAssured.given(emailRequestSpecification)
        .given()
        .delete("/api/v1/messages")
        .then()
        .assertThat()
        .statusCode(200)

      postgreSQLContainer.start()

      val flyway = Flyway.configure().dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
        .load()
      flyway.migrate()



      vertx.deployVerticle(MainVerticle(), testContext.succeeding<String> { _ ->
        GetSession().setPassword()
        val response = GetSession().loginResponse()
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
  @DisplayName("getUser")
  fun getUser() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .get("/api/auth/user")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
    val jsonPath = response.jsonPath()


    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getString("user.email")).isEqualTo("test@admin.com")
    assertThat(jsonPath.getString("user.name")).isEqualTo("Test Admin")


  }



  @Test
  @Order(2)
  @DisplayName("validateRefresh")
  fun validateRefresh() {
    Thread.sleep(1500) //front end will avoid simultaneous refresh - delaying thread here for to avoid conflict
    val data = JsonObject()
      .put("refresh_token", refreshValue)
    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .post("/api/auth/refresh")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
    val jsonPath = response.jsonPath()

    val cookies = response.cookies()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    logger.info("old token ${refreshValue}")

    sessionValue = jsonPath.getString("token")
    refreshValue = jsonPath.getString("refresh_token")
    logger.info("new token ${refreshValue}")
  }


  @Test
  @Order(3)
  @DisplayName("testLogout")
  fun testLogout() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .post("/api/auth/logout")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
    val jsonPath = response.jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

  }


  @Test
  @Order(4)
  @DisplayName("getUsersAfterLogout")
  fun getUsersAfterLogout() {
    RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(false)))
      .get("/api/auth/user")
      .then()
      .assertThat()
      .statusCode(401)


  }




  @Test
  @Order(5)
  @DisplayName("failedRefreshAfterLogout")
  fun failedRefreshAfterLogout() {
    Thread.sleep(1500) //front end will avoid simultaneous refresh - delaying thread here for to avoid conflict
    val data = JsonObject()
      .put("refresh_token", refreshValue)
    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .post("/api/auth/refresh")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).contains("Refresh token has already been used")
  }

  @Test
  @Order(6)
  @DisplayName("testResetPassword")
  fun testResetPassword() {
    val data = JsonObject()
      .put("email", "test@admin.com")

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .post("/api/auth/reset-password")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    val jsonPath2 = RestAssured.given(emailRequestSpecification)
      .given()
      .param("kind", "to")
      .param("query", "test@admin.com")
      .get("/api/v2/search")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath2.getInt("total")).isEqualTo(1)
    assertThat(jsonPath2.getString("items[0].Content.Body")).contains("Dear Test Admin")
  }

  @Test
  @Order(7)
  @DisplayName("reAuthenticateAndRefresh")
  fun reAuthenticateAndRefresh() {
    val response = GetSession().loginResponse()
    val cookies = response.cookies()
    val jsonPath = response.jsonPath()
    sessionValue = jsonPath.getString("token")
    refreshValue = jsonPath.getString("refresh_token")
    Thread.sleep(1500)

    val data = JsonObject()
      .put("refresh_token", refreshValue)
    val response2 = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .post("/api/auth/refresh")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
    val jsonPath2 = response2.jsonPath()

    assertThat(jsonPath2.getBoolean("success")).isTrue()
  }

  @Test
  @Order(8)
  @DisplayName("reuseRefreshTokenForError")
  fun reuseRefreshTokenForError() {
    Thread.sleep(1500)
    val data = JsonObject()
      .put("refresh_token", refreshValue)

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(data.encode())
      .post("/api/auth/refresh")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).contains("Refresh token is not the latest token")
  }
}
