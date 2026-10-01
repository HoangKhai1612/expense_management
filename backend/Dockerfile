# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies against the POM alone first so that editing source does
# not invalidate the dependency layer.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as an unprivileged user rather than root.
RUN addgroup -S finance && adduser -S -G finance finance

COPY --from=build /build/target/finance-ai-backend.jar app.jar
RUN chown -R finance:finance /app
USER finance

EXPOSE 8081

# The database is a separate service, so the health check must tolerate a
# brief window where PostgreSQL is still starting.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -qO- http://127.0.0.1:8081/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
