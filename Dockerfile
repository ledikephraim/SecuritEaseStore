# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
