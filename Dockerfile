# Stage 1: Build JAR using Maven and OpenJDK 21
FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and package JAR (skipping unit tests during image build)
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal, secure JRE 21 runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-privileged user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy compiled executable JAR from builder stage
COPY --from=build /app/target/*.jar app.jar

# Dynamic port assignment for cloud platforms (Railway, Render, Koyeb)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
