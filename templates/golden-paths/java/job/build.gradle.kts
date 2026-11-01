// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java job golden path (P1 task 1.4)
plugins {
    java
    application
    jacoco
}

group = "africa.vigil"
version = "0.1.0"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

application { mainClass = "africa.vigil.templates.job.JobMain" }

dependencies {
    implementation(platform(libs.opentelemetry.bom))
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testImplementation(libs.opentelemetry.sdk.testing)
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
        rule {
            element = "PACKAGE"
            includes = listOf("africa.vigil.templates.job.policy")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
