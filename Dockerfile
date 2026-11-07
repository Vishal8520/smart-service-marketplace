# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress || true
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root system group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/smart-service-marketplace-*.*ar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8081}/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
