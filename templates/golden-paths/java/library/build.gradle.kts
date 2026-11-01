// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Java library golden path (P1 task 1.4)
plugins {
    java
    jacoco
}

group = "africa.vigil"
version = "0.1.0"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    // The library uses the OpenTelemetry API only. The caller owns the SDK and exporter.
    implementation(platform(libs.opentelemetry.bom))
    implementation(libs.opentelemetry.api)

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
        // B8: the policy module is fully covered; this template is small enough to hold 100 %.
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
