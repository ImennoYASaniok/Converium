# Этап сборки
FROM gradle:7.6-jdk17 AS build
WORKDIR /home/gradle/src

COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon --info

# Финальный этап
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]