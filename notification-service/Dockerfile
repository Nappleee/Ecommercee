
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

RUN mvn -pl notification-service -am -DskipTests package


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/notification-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]