// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java service golden path (P1 task 1.4)
plugins {
    java
    jacoco
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "africa.vigil"
version = "0.1.0"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Boot's dependency management owns these versions (1.62.0 with Boot 4.1.2). A platform
    // import here would lose to it and silently pin nothing, so the service declares no version.
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Boot 4 keeps the webmvc test autoconfiguration in its own module.
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
}

dependencyLocking { lockAllConfigurations() }

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

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
        // The policy seam must be fully covered, not merely carried by the rest of the service.
        rule {
            element = "PACKAGE"
            includes = listOf("africa.vigil.templates.service.policy")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
