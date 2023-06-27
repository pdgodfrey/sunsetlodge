package us.pgodfrey.sunsetlodge

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.RedirectConfig
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.specification.RequestSpecification
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.flywaydb.core.Flyway
import org.junit.ClassRule
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

@DisplayName("TestMainVerticle")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestMainVerticle {
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
  @DisplayName("Test ReadyZ")
  fun testReadyZ() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/readyz")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    Assertions.assertThat(jsonPath.getString("status")).isEqualTo("UP")
    Assertions.assertThat(jsonPath.getBoolean("success")).isTrue()
  }

  @Test
  @Order(2)
  @DisplayName("Test HealthZ")
  fun testHealthZ() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/healthz")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    Assertions.assertThat(jsonPath.getString("status")).isEqualTo("UP")
  }
}
