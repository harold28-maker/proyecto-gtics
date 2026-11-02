FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S skillbridge && adduser -S skillbridge -G skillbridge \
    && mkdir -p /app/uploads && chown -R skillbridge:skillbridge /app

COPY --from=build --chown=skillbridge:skillbridge /workspace/target/skillbridge-ai-0.0.1-SNAPSHOT.jar /app/app.jar

USER skillbridge
EXPOSE 8080
VOLUME ["/app/uploads"]

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
