FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN chmod +x mvnw
RUN --mount=type=cache,id=s/0153b728-adaf-4c3a-9ae7-82a4dac5142e/root/.m2,target=/root/.m2 \
    ./mvnw -B -DskipTests dependency:go-offline

COPY src ./src

RUN ./mvnw package -B -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
