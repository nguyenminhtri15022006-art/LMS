# Stage 1: Build source với đầy đủ POM của Maven Reactor
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY service-registry/pom.xml service-registry/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY course-service/pom.xml course-service/pom.xml
COPY assignment-service/pom.xml assignment-service/pom.xml
COPY blog-service/pom.xml blog-service/pom.xml
COPY chatbot-service/pom.xml chatbot-service/pom.xml
COPY web-portal/pom.xml web-portal/pom.xml
COPY web-app/pom.xml web-app/
COPY api-gateway/src api-gateway/src
RUN mvn -f pom.xml clean package -pl api-gateway -am -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget \
    && addgroup -S lmsgroup \
    && adduser -S lmsuser -G lmsgroup

COPY --from=builder /build/api-gateway/target/*.jar app.jar
USER lmsuser

EXPOSE 8080
ENV SERVER_PORT=8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
