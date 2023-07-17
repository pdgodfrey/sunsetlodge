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
import kotlin.collections.LinkedHashMap

@DisplayName("UsersTest")
@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsersTests {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  private var emailRequestSpecification: RequestSpecification? = null
  var sessionValue: String? = null
  var refreshValue: String? = null
  private var httpPort = 0


  val today = LocalDate.now()



  val userObject = JsonObject()
    .put("name", "Test User")
    .put("email", "test@user.com")
    .put("role_id", 2)

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
  @DisplayName("Get Users")
  fun getUsers() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/users")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("Test Admin")
    assertThat(jsonPath.getString("rows[0].email")).isEqualTo("test@admin.com")
    assertThat(jsonPath.getInt("rows[0].role_id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].role_name")).isEqualTo("Administrator")
  }

  @Test
  @Order(2)
  @DisplayName("Create User")
  fun createUser() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(userObject.encode())
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/users")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getString("data.role_name")).isEqualTo("User")

    userObject.put("id", jsonPath.getInt("data.id"))
  }

  @Test
  @Order(3)
  @DisplayName("Invalid Create - No Name or Email")
  fun invalidCreateNoNameOrEmail() {
    val testObject = userObject.copy()
    testObject.remove("name")
    testObject.remove("email")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(testObject.encode())
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/users")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Name is a required field, Email is a required field")
  }

  @Test
  @Order(4)
  @DisplayName("Invalid Create - Invalid Email, no Role")
  fun invalidCreateInvalidEmailNoRole() {
    val testObject = userObject.copy()
    testObject.remove("role_id")
    testObject.put("email", "Foo")

    val response = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(testObject.encode())
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/users")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("Email is invalid, Role is a required field")
  }

  @Test
  @Order(5)
  @DisplayName("Invalid Body Create User")
  fun invalidBodyCreateUser() {
    val response = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/users")
      .then()
      .assertThat()
      .statusCode(400)
      .extract()
      .asString()

    assertThat(response).isEqualTo("No request body")
  }

  @Test
  @Order(6)
  @DisplayName("Get Users After Create")
  fun getUsersAfterCreate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/users")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("Test Admin")
    assertThat(jsonPath.getString("rows[0].email")).isEqualTo("test@admin.com")
    assertThat(jsonPath.getInt("rows[0].role_id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].role_name")).isEqualTo("Administrator")

    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(2)
    assertThat(jsonPath.getString("rows[1].name")).isEqualTo("Test User")
    assertThat(jsonPath.getString("rows[1].email")).isEqualTo("test@user.com")
    assertThat(jsonPath.getInt("rows[1].role_id")).isEqualTo(2)
    assertThat(jsonPath.getString("rows[1].role_name")).isEqualTo("User")
  }

  @Test
  @Order(7)
  @DisplayName("Update User")
  fun updateUser() {
    userObject.put("name", "Test User Converted")
    userObject.put("role_id", 1)

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(userObject.encode())
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/api/users/${userObject.getInteger("id")}")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()

    assertThat(jsonPath.getString("data.role_name")).isEqualTo("Administrator")
  }

  @Test
  @Order(8)
  @DisplayName("Reset User Password")
  fun resetUserPassword() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .queryParam("id", userObject.getInteger("id"))
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/api/users/reset-password")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()


    val jsonPath2 = RestAssured.given(emailRequestSpecification)
      .given()
      .param("kind", "to")
      .param("query", "test@user.com")
      .header("Authorization", "Bearer ${sessionValue}")
      .get("/api/v2/search")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath2.getInt("total")).isEqualTo(1)
    assertThat(jsonPath2.getString("items[0].Content.Body")).contains("Dear Test User Converted")
  }

  @Test
  @Order(9)
  @DisplayName("Get Users After Update")
  fun getUsersAfterUpdate() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/users")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(2)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("Test Admin")
    assertThat(jsonPath.getString("rows[0].email")).isEqualTo("test@admin.com")
    assertThat(jsonPath.getInt("rows[0].role_id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].role_name")).isEqualTo("Administrator")

    assertThat(jsonPath.getInt("rows[1].id")).isEqualTo(2)
    assertThat(jsonPath.getString("rows[1].name")).isEqualTo("Test User Converted")
    assertThat(jsonPath.getString("rows[1].email")).isEqualTo("test@user.com")
    assertThat(jsonPath.getInt("rows[1].role_id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[1].role_name")).isEqualTo("Administrator")
  }

  @Test
  @Order(10)
  @DisplayName("Delete User")
  fun deleteUser() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .delete("/api/users/${userObject.getInteger("id")}")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
  }


  @Test
  @Order(11)
  @DisplayName("Get Users After Delete")
  fun getUsersAfterDelete() {
    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .header("Authorization", "Bearer ${sessionValue}")
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .get("/api/users")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    assertThat(jsonPath.getBoolean("success")).isTrue()
    assertThat(jsonPath.getList<Any>("rows").size).isEqualTo(1)

    assertThat(jsonPath.getInt("rows[0].id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].name")).isEqualTo("Test Admin")
    assertThat(jsonPath.getString("rows[0].email")).isEqualTo("test@admin.com")
    assertThat(jsonPath.getInt("rows[0].role_id")).isEqualTo(1)
    assertThat(jsonPath.getString("rows[0].role_name")).isEqualTo("Administrator")
  }
}
