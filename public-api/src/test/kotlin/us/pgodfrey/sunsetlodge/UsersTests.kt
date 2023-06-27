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
import kotlin.collections.LinkedHashMap

@DisplayName("UsersTest")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsersTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
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



      if(httpPort == 8081) {
        postgreSQLContainer.start()



        val flyway = Flyway.configure().dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
          .load()
        flyway.migrate()



        vertx.deployVerticle(MainVerticle(), testContext.succeeding<String> { _ ->
          val cookies = GetSession().getAuthCookies()
          sessionValue = cookies["auth-token"]
          refreshValue = cookies["refresh-token"]

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

//  @Test
//  @Order(1)
//  @DisplayName("validateRefresh")
//  fun validateRefresh() {
//    Thread.sleep(1500) //front end will avoid simultaneous refresh - delaying thread here for to avoid conflict
//    val response = RestAssured.given(requestSpecification)
//      .given()
//      .param("refresh_token", refreshValue)
//      .post("/auth/refresh")
//      .then()
//      .assertThat()
//      .statusCode(200)
//      .extract()
//    val jsonPath = response.jsonPath()
//
//    val cookies = response.cookies()
//
//    assertThat(jsonPath.getBoolean("success")).isTrue()
//
//
//    sessionValue = cookies["auth-token"]
//    refreshValue = cookies["refresh-token"]
//  }
//
//
//
//
//  @Test
//  @Order(2)
//  @DisplayName("testLogout")
//  fun testLogout() {
//    val response = RestAssured.given(requestSpecification)
//      .given()
//      .header("Authorization", "Bearer ${sessionValue}")
//      .post("/auth/logout")
//      .then()
//      .assertThat()
//      .statusCode(200)
//      .extract()
//    val jsonPath = response.jsonPath()
//
//    assertThat(jsonPath.getBoolean("success")).isTrue()
//
//  }
//
//
//
//
//  @Test
//  @Order(3)
//  @DisplayName("failedRefreshAfterLogout")
//  fun failedRefreshAfterLogout() {
//    Thread.sleep(1500) //front end will avoid simultaneous refresh - delaying thread here for to avoid conflict
//    val response = RestAssured.given(requestSpecification)
//      .given()
//      .param("refresh_token", refreshValue)
//      .post("/auth/refresh")
//      .then()
//      .assertThat()
//      .statusCode(400)
//      .extract()
//      .asString()
//
//    assertThat(response).contains("Refresh token has already been used")
//  }
}
