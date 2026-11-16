FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn

RUN mvn -B -ntp dependency:go-offline

COPY src src

RUN mvn -B -ntp -Dmaven.test.skip=true package


FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system gymbro \
    && useradd --system --gid gymbro --home-dir /app gymbro

WORKDIR /app

COPY --from=build \
    --chown=gymbro:gymbro \
    /workspace/target/gympilot-backend-*.jar \
    app.jar

USER gymbro

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

CMD ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]