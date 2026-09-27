// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Java mission runtime
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
}
group = "africa.vigil"
version = "0.1.0"
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
repositories { mavenCentral() }
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
sourceSets {
    main { java.setSrcDirs(listOf("services/core/src/main/java")); resources.setSrcDirs(listOf("services/core/src/main/resources")) }
    test { java.setSrcDirs(listOf("services/core/src/test/java")) }
}
dependencyLocking { lockAllConfigurations() }
tasks.test { useJUnitPlatform(); finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport { dependsOn(tasks.test); reports { xml.required = true; html.required = true } }
spotless { java { target("services/core/src/**/*.java"); googleJavaFormat("1.28.0") } }
