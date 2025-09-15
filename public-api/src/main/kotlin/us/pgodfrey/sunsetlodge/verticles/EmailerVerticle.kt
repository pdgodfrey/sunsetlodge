package us.pgodfrey.sunsetlodge.verticles

import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.mail.*
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait

class EmailerVerticle: CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)

  private var emailNotificationHttpPort = 0
  private var emailNotificationHost = ""
  private var tlsRequired = false
  private var emailUsername = ""
  private var emailPassword = ""


  private lateinit var webClient: WebClient
  private lateinit var engine: HandlebarsTemplateEngine
  private lateinit var mailClient: MailClient

  private lateinit var DEFAULT_FROM: String

  override suspend fun start() {
    val env = System.getenv()

    emailNotificationHttpPort = env.getOrDefault("EMAIL_HTTP_PORT", "1025").toInt()
    emailNotificationHost = env.getOrDefault("EMAIL_HOST", "localhost")
    tlsRequired = env.getOrDefault("EMAIL_TLS_REQUIRED", "false").toBoolean()
    emailUsername = env.getOrDefault("EMAIL_USERNAME", "")
    emailPassword = env.getOrDefault("EMAIL_PASSWORD", "")


//    DEFAULT_FROM = env.getOrDefault("DEFAULT_FROM", "noreply@sunsetlodge.org")
    DEFAULT_FROM = env.getOrDefault("DEFAULT_FROM", "sunset@pgodfrey.us")

    val mailConfig = MailConfig()
      .setHostname(emailNotificationHost)
      .setPort(emailNotificationHttpPort)

    if(tlsRequired) {
      mailConfig.setStarttls(StartTLSOptions.REQUIRED)
      mailConfig.setSsl(false)
    }

    if(emailUsername.isNotEmpty() && emailPassword.isNotEmpty()) {
      mailConfig.setLogin(LoginOption.REQUIRED)
      mailConfig.setUsername(emailUsername)
      mailConfig.setPassword(
        emailPassword
      )
    }

    mailClient = MailClient.createShared(
      vertx,
      mailConfig
    )
    webClient = WebClient.create(vertx)
    engine = HandlebarsTemplateEngine.create(vertx)

    val eb = vertx.eventBus()

    val consumer = eb.consumer<Any>("email.send")
    consumer.handler(this::sendEmail)
  }

  private fun sendEmail(message: Message<Any>) {
    val messageBody = message.body() as JsonObject

    val from: String =
      if (messageBody.getString("from") != null) messageBody.getString("from") else DEFAULT_FROM

    val toList = ArrayList<String>()
    toList.add(messageBody.getString("recipient_email"))

    val mailMessage: MailMessage = MailMessage()
      .setFrom(from)
      .setTo(toList)
      .setSubject(messageBody.getString("subject", ""))

    engine.render(messageBody, "email_templates/${messageBody.getString("template")}") {
      if(it.succeeded()){
        logger.info("succeeded")


        mailMessage.setHtml(it.result().toString())

        mailClient.sendMail(mailMessage)
          .onComplete { mailResult ->
            if(mailResult.succeeded()){
              logger.info("sent")
              logger.info(mailResult.result().messageID)
              logger.info(mailResult.result().recipients.joinToString(", "))
            } else {
              logger.info("not sent")
              logger.info(mailResult.cause().message)
              logger.info(mailResult.cause().stackTraceToString())
              mailResult.cause().printStackTrace()
            }

            message.reply("done")
          }
      } else {
        it.cause().printStackTrace()
        message.fail(500, it.cause().message)
      }

    }

  }
}
