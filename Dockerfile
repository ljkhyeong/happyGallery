FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY application ./application
COPY adapter-in-web ./adapter-in-web
COPY adapter-out-persistence ./adapter-out-persistence
COPY adapter-out-external ./adapter-out-external
COPY domain ./domain
COPY bootstrap ./bootstrap
RUN chmod +x gradlew && ./gradlew :bootstrap:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/bootstrap/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
