import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.7.21"
  application
  id("org.flywaydb.flyway") version "9.8.2"
  id("org.jetbrains.dokka") version "1.8.20"
}

group = "us.pgodfrey"
version = "1.0.0-SNAPSHOT"

val mainVerticleName = "us.pgodfrey.sunsetlodge.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"


application {
  mainClass.set(launcherClassName)
}

dependencies {
  val vertxVersion = project.extra["vertxVersion"]
  val junitJupiterVersion = project.extra["junitJupiterVersion"]
  val logbackVersion = project.extra["logbackVersion"]
  val assertjVersion = project.extra["assertjVersion"]
  val restAssuredVersion = project.extra["restAssuredVersion"]
  val testContainersVersion = project.extra["testContainersVersion"]
  val flywayVersion = project.extra["flywayVersion"]

  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-pg-client")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("io.vertx:vertx-web-templ-handlebars")
  implementation("io.vertx:vertx-auth-jwt")
  implementation("io.vertx:vertx-auth-sql-client")
  implementation("io.vertx:vertx-mail-client")
  implementation("io.vertx:vertx-web-client")
  implementation(kotlin("stdlib-jdk8"))
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")

  implementation("com.ongres.scram:client:2.1")


  implementation("ch.qos.logback:logback-core:$logbackVersion")
  implementation("ch.qos.logback:logback-classic:$logbackVersion")


  implementation("org.postgresql:postgresql:42.7.2")
  implementation("org.flywaydb:flyway-core:$flywayVersion")

  implementation("com.google.code.gson:gson:2.11.0")


  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
  testImplementation("org.assertj:assertj-core:$assertjVersion")
  testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
  testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
  testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:elasticsearch:$testContainersVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

//tasks.register<ShadowJar>("shadowJar") {
//  archiveClassifier.set("fat")
//  manifest {
//    attributes(mapOf("Main-Verticle" to mainVerticleName))
//  }
//  mergeServiceFiles()
//  configurations = listOf(project.configurations.runtimeClasspath.get())
//}
//

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }

  from("webroot") {
    // Copy Foo file into Bar/ in the shadowed JAR.
    into("webroot")
  }

  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  environment("DISPLAY_SQL_ERRORS", "true")
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}

flyway {
  url = "jdbc:postgresql://localhost/sunsetlodge"
  user = "sunset"
  password = "abc123"
  locations = arrayOf("filesystem:./src/main/resources/db/migration")
}
