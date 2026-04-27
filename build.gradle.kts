plugins {
    java
    checkstyle
    id("com.diffplug.spotless") version "7.2.1"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "ru.spbstu.booktracker"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val springVersion = "7.0.0"
val springDataMongoVersion = "5.0.0"
val jacksonVersion = "2.18.2"
val jettyVersion = "12.0.16"
val jakartaServletVersion = "6.0.0"
val slf4jVersion = "2.0.16"
val logbackVersion = "1.5.12"
val jeromqVersion = "0.6.0"
val junitVersion = "5.11.3"
val mockitoVersion = "5.20.0"
val assertjVersion = "3.26.3"
val testcontainersVersion = "1.20.4"
val springRestDocsVersion = "3.0.3"
val mongoDriverVersion = "5.6.1"

dependencies {
    // Spring Framework 7 (NO Spring Boot)
    implementation("org.springframework:spring-context:$springVersion")
    implementation("org.springframework:spring-beans:$springVersion")
    implementation("org.springframework:spring-core:$springVersion")
    implementation("org.springframework:spring-web:$springVersion")
    implementation("org.springframework:spring-webmvc:$springVersion")

    // Spring Data MongoDB
    implementation("org.springframework.data:spring-data-mongodb:$springDataMongoVersion")
    implementation("org.mongodb:mongodb-driver-sync:$mongoDriverVersion")

    // Jakarta Servlet API (provided by Jetty EE10 at runtime, but needed at compile-time)
    implementation("jakarta.servlet:jakarta.servlet-api:$jakartaServletVersion")

    // Embedded Jetty 12 (EE10 = jakarta.servlet 6)
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:$jettyVersion")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-webapp:$jettyVersion")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // ZeroMQ (Java native)
    implementation("org.zeromq:jeromq:$jeromqVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Validation helper
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.springframework:spring-test:$springVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:mongodb:$testcontainersVersion")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc:$springRestDocsVersion")
}

tasks.test {
    useJUnitPlatform()
    // Allow ByteBuddy / Mockito to handle the latest class-file version on JDK 25.
    systemProperty("net.bytebuddy.experimental", "true")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

checkstyle {
    toolVersion = "10.20.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.named<Checkstyle>("checkstyleMain") {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}
tasks.named<Checkstyle>("checkstyleTest") {
    enabled = false
}

spotless {
    java {
        googleJavaFormat("1.27.0")
        target("src/**/*.java")
        targetExclude("build/**/*.java")
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    archiveBaseName.set("book-tracker-advisor-bot")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "ru.spbstu.booktracker.Application"
        attributes["Multi-Release"] = "true"
    }
    mergeServiceFiles()
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named("build") {
    dependsOn("shadowJar")
}
