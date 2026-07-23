FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG APP_JAR
RUN apk upgrade --no-cache
COPY ${APP_JAR} app.jar

ENV HOME=/tmp
EXPOSE 8080 8081
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
