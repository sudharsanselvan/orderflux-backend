# ============================================================
# Stage 1: BUILD
# ============================================================
# Use Maven + Java 21 to compile and package

FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first — Docker caches dependency layer
# If pom.xml unchanged → dependencies not re-downloaded
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: RUNTIME
# ============================================================
# Use only JRE — smaller and more secure than full JDK
# Alpine = minimal Linux (~5MB base)

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user — security best practice
# Never run apps as root inside containers
RUN addgroup -S orderflux && \
    adduser -S orderflux -G orderflux

# Copy ONLY the JAR from builder stage
# Source code, Maven, JDK never reach this image
COPY --from=builder /app/target/*.jar app.jar

# Give ownership to non-root user
RUN chown orderflux:orderflux app.jar

# Switch to non-root user
USER orderflux

# Document port — actual publishing done in docker-compose
EXPOSE 8080

# Health check — Docker monitors container health
HEALTHCHECK \
  --interval=30s \
  --timeout=5s \
  --start-period=60s \
  --retries=3 \
  CMD wget -q -O- http://localhost:8080/api/health/ping \
      || exit 1

# Start application with production JVM flags
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]