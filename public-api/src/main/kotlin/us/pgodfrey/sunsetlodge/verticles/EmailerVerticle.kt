package us.pgodfrey.sunsetlodge.verticles

import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait

class EmailerVerticle: CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)

  private var emailNotificationHttpPort = 0
  private var emailNotificationHost = ""


  private lateinit var webClient: WebClient
  private lateinit var engine: HandlebarsTemplateEngine
  private lateinit var mailClient: MailClient

  private lateinit var DEFAULT_FROM: String

  override suspend fun start() {
    val env = System.getenv()

    emailNotificationHttpPort = env.getOrDefault("EMAIL_HTTP_PORT", "1025").toInt()
    emailNotificationHost = env.getOrDefault("EMAIL_HOST", "localhost")

    DEFAULT_FROM = env.getOrDefault("DEFAULT_FROM", "noreply@sunsetlodge.org")

    mailClient = MailClient.createShared(
      vertx,
      MailConfig()
        .setHostname(emailNotificationHost)
        .setPort(emailNotificationHttpPort)
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
            } else {
              logger.info("not sent")
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
