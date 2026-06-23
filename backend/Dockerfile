# syntax=docker/dockerfile:1.6
# ─────────────────────────────────────────────────────────
# Stage 1: Build (Maven + JDK 21)
# ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencies layer
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e dependency:go-offline

# Build application
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests package

# ─────────────────────────────────────────────────────────
# Stage 2: Runtime (Eclipse Temurin JRE 21, slim)
# ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Run as non-root user
RUN groupadd --system --gid 1001 blog && \
    useradd --system --uid 1001 --gid blog --home-dir /app --shell /usr/sbin/nologin blog && \
    mkdir -p /app/uploads && \
    chown -R blog:blog /app

WORKDIR /app

# Copy built JAR from builder
COPY --from=builder --chown=blog:blog /build/target/blog-cms-backend.jar /app/app.jar

# Health check (Spring Actuator)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

USER blog

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
