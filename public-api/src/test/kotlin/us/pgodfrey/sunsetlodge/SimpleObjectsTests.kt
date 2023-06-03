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
class SimpleObjectsTests {
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
  @DisplayName("Test Get Buildings")
  fun getBuildings() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/buildings")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(6)

    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("All Buildings")
    assertThat(jsonPath.getInt("rows[0].max_occupancy")).isEqualTo(26)
    assertThat(jsonPath.getInt("rows[0].bedrooms")).isEqualTo(13)
    assertThat(jsonPath.getInt("rows[0].bathrooms")).isEqualTo(9)

    assertThat(jsonPath.getString("rows[1].name")).isEqualTo("The Lodge")
    assertThat(jsonPath.getInt("rows[1].max_occupancy")).isEqualTo(10)
    assertThat(jsonPath.getInt("rows[1].bedrooms")).isEqualTo(5)
    assertThat(jsonPath.getInt("rows[1].bathrooms")).isEqualTo(4)

    assertThat(jsonPath.getString("rows[2].name")).isEqualTo("Lakeside Cabin")
    assertThat(jsonPath.getInt("rows[2].max_occupancy")).isEqualTo(6)
    assertThat(jsonPath.getInt("rows[2].bedrooms")).isEqualTo(3)
    assertThat(jsonPath.getInt("rows[2].bathrooms")).isEqualTo(2)

    assertThat(jsonPath.getString("rows[3].name")).isEqualTo("Treehouse Cabin")
    assertThat(jsonPath.getInt("rows[3].max_occupancy")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[3].bedrooms")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[3].bathrooms")).isEqualTo(1)

    assertThat(jsonPath.getString("rows[4].name")).isEqualTo("Bird's Nest Cabin")
    assertThat(jsonPath.getInt("rows[4].max_occupancy")).isEqualTo(2)
    assertThat(jsonPath.getInt("rows[4].bedrooms")).isEqualTo(1)
    assertThat(jsonPath.getInt("rows[4].bathrooms")).isEqualTo(1)

    assertThat(jsonPath.getString("rows[5].name")).isEqualTo("Judy's Cabin")
    assertThat(jsonPath.getInt("rows[5].max_occupancy")).isEqualTo(6)
    assertThat(jsonPath.getInt("rows[5].bedrooms")).isEqualTo(3)
    assertThat(jsonPath.getInt("rows[5].bathrooms")).isEqualTo(1)
  }
}
