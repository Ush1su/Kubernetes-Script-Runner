# ---- Build stage ----
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

# Copy only build files first to leverage Docker layer caching
COPY build.gradle.kts settings.gradle.kts gradle.properties* /app/
COPY gradle /app/gradle

# Download dependencies (will be cached unless build files change)
RUN gradle --no-daemon dependencies

# Copy source and build
COPY src /app/src
RUN gradle --no-daemon clean bootJar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the generated jar
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]