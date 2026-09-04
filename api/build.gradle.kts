plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    // The site is plain static assets; serving them from the classpath keeps deployment to one jar.
    runtimeOnly(project(":web"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.assertj)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(testFixtures(project(":lib")))
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.1")
        }
    }
}

application {
    mainClass = "net.niebes.sudoku.api.MainKt"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
