# Stage 1: Build application with Maven
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Install maven and package the jar without tests
RUN apk add --no-cache maven && mvn clean package -DskipTests

# Stage 2: Production JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built jar from build stage
COPY --from=build /app/target/xpense-backend-*.jar app.jar

# Render passes dynamic PORT
ENV PORT=8080
EXPOSE 8080

# Run with container-aware memory allocation (prevents Render 512MB RAM OOM)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
