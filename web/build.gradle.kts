// The site itself: static assets only, packaged onto the classpath so `api` can serve them.
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
