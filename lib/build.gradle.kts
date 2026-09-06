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

// Finds, for each technique, the corpus puzzle and step where the site can show it live; the
// browser regenerates the replay itself, so the output is only coordinates. Lives on the test
// classpath because that is where the corpora live.
val findTechniqueExamples by tasks.registering(JavaExec::class) {
    val jvmTest = kotlin.targets.getByName("jvm").compilations.getByName("test")
    classpath(jvmTest.output.allOutputs, jvmTest.runtimeDependencyFiles)
    mainClass = "net.niebes.sudoku.examples.ExampleFinderKt"
    val outFile = layout.buildDirectory.file("technique-examples/examples.js")
    outputs.file(outFile)
    argumentProviders.add { listOf(outFile.get().asFile.absolutePath) }
}
