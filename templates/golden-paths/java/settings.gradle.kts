// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java golden-path template build (P1 task 1.4)
pluginManagement {
    repositories { gradlePluginPortal() }
    plugins {
        id("org.springframework.boot") version "4.1.2"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

rootProject.name = "vigil-java-templates"
include("library", "service", "job")
