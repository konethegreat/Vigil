// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Compile generated Java and Kotlin contract stubs (P2 task 2.5)
plugins {
    java
    kotlin("jvm") version "2.3.20"
}

group = "africa.vigil"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
}

sourceSets {
    main {
        java.srcDir("../gen/java")
        kotlin.srcDir("../gen/kotlin")
    }
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:4.33.1")
    implementation("com.google.protobuf:protobuf-kotlin:4.33.1")
}

dependencyLocking {
    lockAllConfigurations()
}
