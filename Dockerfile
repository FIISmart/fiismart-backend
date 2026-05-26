# syntax=docker/dockerfile:1.7
# Multi-stage build: Maven build → slim JRE runtime.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Cache dependencies first.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Spring Boot fat jar; pick whichever Maven produced.
COPY --from=build /src/target/*.jar app.jar

# Railway / Cloud Run / Render inject PORT — application-prod.properties
# binds server.port=${PORT:8080}.
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
