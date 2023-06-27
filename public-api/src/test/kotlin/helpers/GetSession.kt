package helpers

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.RedirectConfig
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.session.SessionFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class GetSession {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)
  private var requestSpecification: RequestSpecification? = null
  private var httpPort = 0

  init {

    val env = System.getenv()
    httpPort = env.getOrDefault("LOGIN_HTTP_PORT", "8081").toInt()

    //create request specification
    requestSpecification = RequestSpecBuilder()
      .addFilters(Arrays.asList(ResponseLoggingFilter(), RequestLoggingFilter()))
      .setBaseUri("http://localhost:${httpPort}/")
      .build()
  }

  fun getAuthCookies(): MutableMap<String, String> {
    setPassword()

    val loginData = JsonObject()
      .put("email", "test@admin.com")
      .put("password", "tester")

    val cookies = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(loginData.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .post("/auth/authenticate")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .cookies()

    logger.info("cookies : {}", cookies)

    return cookies
  }

  private fun setPassword() {
    val setPasswordData = JsonObject()
      .put("email", "test@admin.com")
      .put("reset_token", "123")
      .put("password", "tester")

    val jsonPath = RestAssured.given(requestSpecification)
      .given()
      .contentType(ContentType.JSON)
      .body(setPasswordData.encode())
      .config(RestAssured.config().redirect(RedirectConfig().followRedirects(true)))
      .put("/auth/set-password")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath()

    Assertions.assertThat(jsonPath.getBoolean("success")).isTrue()
  }
}
