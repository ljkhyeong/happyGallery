FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

ARG APP_JAR
RUN apk upgrade --no-cache
RUN touch /app/media-backup-guard-v1 && chmod 0444 /app/media-backup-guard-v1
COPY --chown=10001:10001 --chmod=0440 ${APP_JAR} /app/app.jar

ENV HOME=/tmp
EXPOSE 8080 8081
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
