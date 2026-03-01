# Этап сборки
FROM gradle:7.6-jdk17 AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar --no-daemon --info

# Этап выполнения
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]