FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY dentis-common/pom.xml dentis-common/
COPY dentis-patient/pom.xml dentis-patient/
COPY dentis-scheduling/pom.xml dentis-scheduling/
COPY dentis-clinical/pom.xml dentis-clinical/
COPY dentis-billing/pom.xml dentis-billing/
COPY dentis-notification/pom.xml dentis-notification/
COPY dentis-clinic/pom.xml dentis-clinic/
COPY dentis-api/pom.xml dentis-api/

RUN mvn dependency:go-offline -q 2>/dev/null || true

COPY dentis-common/src dentis-common/src
COPY dentis-patient/src dentis-patient/src
COPY dentis-scheduling/src dentis-scheduling/src
COPY dentis-clinical/src dentis-clinical/src
COPY dentis-billing/src dentis-billing/src
COPY dentis-notification/src dentis-notification/src
COPY dentis-clinic/src dentis-clinic/src
COPY dentis-api/src dentis-api/src

RUN mvn clean package -DskipTests -pl dentis-api -am -q \
    && executable_jar="$(find /workspace/dentis-api/target \
        -maxdepth 1 \
        -type f \
        -name 'dentis-api-*.jar' \
        ! -name '*.jar.original' | head -n 1)" \
    && test -n "$executable_jar" \
    && cp "$executable_jar" /workspace/dentis-api-exec.jar

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S dentis && adduser -S dentis -G dentis

WORKDIR /app

COPY --from=builder /workspace/dentis-api-exec.jar app.jar

RUN chown -R dentis:dentis /app
USER dentis

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
