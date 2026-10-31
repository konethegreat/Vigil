# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Java core build and runtime
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - Match the Gradle wrapper version (9.8.2)
FROM gradle:9.8.2-jdk25@sha256:e46e15353757ef9c9be0197a243653e88d25ebcd4c3c841057cf839c11fcf898 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts gradle.properties gradle.lockfile ./
COPY services/core services/core
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:25-jre@sha256:bb036ed6cfdc57e3da7c22634d15f1b840d2caf76183861c80e81ca4b5104abb AS runtime
WORKDIR /app
COPY --from=build /workspace/build/libs/vigil-core-0.1.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
