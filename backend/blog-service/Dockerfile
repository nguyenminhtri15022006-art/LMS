# Stage 1: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

ARG JAR_FILE=blog-service/target/*.jar
COPY ${JAR_FILE} app.jar

ENV SERVER_PORT=8084
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
