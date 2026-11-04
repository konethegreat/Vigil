// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Gradle settings for the Java and Kotlin contract codegen harness (P2 task 2.5)
pluginManagement {
    repositories { gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

rootProject.name = "vigil-contracts-codegen-check"
