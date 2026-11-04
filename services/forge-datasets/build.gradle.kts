// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Forge Datasets service Gradle build with 85% JaCoCo coverage gate (P4 tasks 4.1-4.5, 4.7)
plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.2"
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
dependencyLocking { lockAllConfigurations() }
configurations.configureEach {
    resolutionStrategy.force("com.google.guava:guava:33.7.2-jre")
}
tasks.bootJar { enabled = false }
tasks.jar { enabled = true }
tasks.test { useJUnitPlatform(); finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport { dependsOn(tasks.test); reports { xml.required = true; html.required = true } }
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
spotless { java { target("src/**/*.java"); googleJavaFormat("1.30.0") } }
