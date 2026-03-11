plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    id("org.jmailen.kotlinter")
    application
}

group = "com.fugisawa.quemfaz"
version = "1.0.0"
application {
    mainClass.set("com.fugisawa.quemfaz.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation("io.ktor:ktor-server-content-negotiation-jvm:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:${libs.versions.ktor.get()}")
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation("org.jetbrains.exposed:exposed-json:${libs.versions.exposed.get()}")

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
    testImplementation("io.insert-koin:koin-test-junit5:${libs.versions.koin.get()}")
    testImplementation(libs.testcontainersPostgresql)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(libs.testcontainers)
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}