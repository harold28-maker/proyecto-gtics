# Construcción reproducible con Java 21 (el proyecto usa --release 17).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 skillbridge && mkdir /app/uploads && chown 10001:10001 /app/uploads
COPY --from=build /app/target/skillbridge-ai-0.0.1-SNAPSHOT.jar /app/skillbridge.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/skillbridge.jar"]
