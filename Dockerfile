# syntax=docker/dockerfile:1

# ---- Build: compila e gera o jar com o JDK completo ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependências primeiro: enquanto o pom.xml não mudar, essa camada vem do cache.
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# Separa o jar em camadas (dependências, loader, aplicação) para o Docker reaproveitar as que não mudaram.
RUN java -Djarmode=tools -jar target/taskhub-*.jar extract --layers --launcher --destination target/extracted

# ---- Runtime: só o JRE, sem Maven nem código-fonte ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER spring

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
