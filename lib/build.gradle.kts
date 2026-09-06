plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    // The JVM target exists for the test suite - JUnit and AssertJ run here, over the same
    // common code the site ships.
    jvm()

    // The solver the site ships: one plain script that registers `sudokuSolver` on the page.
    js {
        browser {
            webpackTask {
                mainOutputFileName = "solver.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(libs.assertj)
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
