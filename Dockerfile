FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY common/pom.xml ./common/pom.xml
COPY common/src ./common/src
RUN mvn -f common/pom.xml install -DskipTests -B

COPY household-service/pom.xml ./household-service/pom.xml
RUN mvn -f household-service/pom.xml dependency:go-offline -B

COPY household-service/src ./household-service/src
RUN mvn -f household-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/household-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]