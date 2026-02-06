# Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on (Render sets PORT env var)
ENV PORT=5000
EXPOSE 5000

# Use exec form to handle signals correctly
# Limit memory to ~350MB to fit in 512MB container (Render Free Tier)
ENTRYPOINT ["java", "-Xmx356m", "-Xss512k", "-XX:CICompilerCount=2", "-Dserver.port=${PORT}", "-jar", "app.jar"]
