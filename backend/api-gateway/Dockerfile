# Stage 1: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

ARG JAR_FILE=api-gateway/target/*.jar
COPY ${JAR_FILE} app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
