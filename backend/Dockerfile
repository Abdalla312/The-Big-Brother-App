FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw package -B -DskipTests \
    && echo "=== FLYWAY MIGRATIONS ===" \
    && jar tf target/*.jar | grep 'BOOT-INF/classes/db/migration/' \
    || (echo "ERROR: No Flyway migrations found in jar!" && exit 1)

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]