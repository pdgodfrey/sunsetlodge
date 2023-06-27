plugins {
  id("com.github.johnrengelman.shadow") version "7.1.2" apply false
  id("com.adarshr.test-logger") version "2.0.0" apply false
  id("com.github.node-gradle.node") version "5.0.0" apply false
  id("org.gradle.test-retry") version "1.1.4" apply false
}

allprojects {
  extra["vertxVersion"] = if (project.hasProperty("vertxVersion")) project.property("vertxVersion") else "4.4.4"
  extra["junitJupiterVersion"] = "5.9.1"
  extra["restAssuredVersion"] = "5.3.0"
  extra["logbackVersion"] = "1.2.11"
  extra["assertjVersion"] = "3.22.0"
  extra["testContainersVersion"] = "1.16.3"
  extra["flywayVersion"] = "8.5.2"
}


subprojects {
  repositories {
    mavenCentral()
    maven("https://dl.bintray.com/jetbrains/markdown/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
  }

  apply(plugin = "java")
  apply(plugin = "application")
  apply(plugin = "com.github.johnrengelman.shadow")
  apply(plugin = "com.adarshr.test-logger")

  tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }

  tasks.named<Test>("test") {
    reports.html.isEnabled = false
  }
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  gradleVersion = "6.6.1"
}


//repositories {
//  mavenCentral()
//}
//
//val vertxVersion = "4.4.1"
//val junitJupiterVersion = "5.7.0"
//val logbackVersion = "1.4.5"
//val flywayVersion = "9.8.2"
//
//
//dependencies {
//  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
//  implementation("io.vertx:vertx-web")
//  implementation("io.vertx:vertx-pg-client")
//  implementation("io.vertx:vertx-lang-kotlin-coroutines")
//  implementation("io.vertx:vertx-lang-kotlin")
//  implementation("io.vertx:vertx-web-templ-handlebars")
//  implementation(kotlin("stdlib-jdk8"))
//  testImplementation("io.vertx:vertx-junit5")
//  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
//
//  implementation("com.ongres.scram:client:2.1")
//
//
//  implementation("ch.qos.logback:logback-core:$logbackVersion")
//  implementation("ch.qos.logback:logback-classic:$logbackVersion")
//
//
//  implementation("org.postgresql:postgresql:42.5.0")
//  implementation("org.flywaydb:flyway-core:$flywayVersion")
//}
//
