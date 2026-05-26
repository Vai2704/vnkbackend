# Dockerfile

# --- STAGE 1: Build the application ---
# Use a JDK image (e.g., eclipse-temurin for better support)
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# 1. Copy pom.xml and download dependencies to utilize caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# 2. Copy source code and build
COPY src src
RUN ./mvnw clean package -DskipTests

# --- STAGE 2: Create the final lightweight image ---
# Use a JRE image for a smaller runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Force the JVM to utilize 80% of container memory statically (Gold Standard)
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=80.0 -XX:MaxRAMPercentage=80.0"

# 3. Expose the application port
EXPOSE 8080

# 4. Copy the built JAR file from the 'build' stage
COPY --from=build /app/target/vnk-server.jar /vnk-server.jar

# 5. Define the entry point
ENTRYPOINT ["java", "-Dspring.profiles.active=staging", "-jar", "/vnk-server.jar"]
