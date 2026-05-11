# Этап сборки
FROM gradle:7.6-jdk17 AS build
WORKDIR /home/gradle/src

COPY gradlew gradle build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon --info

# Финальный этап
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]