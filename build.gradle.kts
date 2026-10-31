// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Java mission runtime
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Spring Boot 4.0.9 security patch
// Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Force patched Guava in the formatter toolchain
plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.4"
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
configurations.configureEach {
    // google-java-format 1.28.0 pulls Guava 32.1.3 (CVE-2026-102554); 33.7.2 is binary compatible.
    resolutionStrategy.force("com.google.guava:guava:33.7.2-jre")
}
tasks.test { useJUnitPlatform(); finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport { dependsOn(tasks.test); reports { xml.required = true; html.required = true } }
spotless { java { target("services/core/src/**/*.java"); googleJavaFormat("1.28.0") } }
