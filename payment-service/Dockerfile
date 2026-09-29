FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

RUN mvn -pl payment-service -am -DskipTests package


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/payment-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]