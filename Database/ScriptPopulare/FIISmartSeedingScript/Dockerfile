FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/FIISmartSeedingScript-1.0-SNAPSHOT.jar app.jar

COPY .env .env

ENTRYPOINT ["java", "-jar", "app.jar"]