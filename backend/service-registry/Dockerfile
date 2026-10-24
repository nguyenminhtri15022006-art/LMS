# Stage 1: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

ARG JAR_FILE=service-registry/target/*.jar
COPY ${JAR_FILE} app.jar

ENV SERVER_PORT=8761
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
