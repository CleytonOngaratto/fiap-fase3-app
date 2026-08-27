# syntax=docker/dockerfile:1

# ============================================================
# Stage 1 — Build
# Compila DENTRO do container (ambiente limpo e reprodutível,
# imune ao bug do IDE local que corrompe target/classes).
# Testes rodam no CI, NÃO aqui (senão o build exigiria banco).
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# pom primeiro (camada cacheável); cache mount no /root/.m2 faz as dependências
# persistirem entre builds → rebuilds (e o demo do vídeo) ficam rápidos.
COPY pom.xml ./

# Maven Central e não download.newrelic.com: o runtime não tem curl/wget, e o build já fala com
# esse endpoint — um segundo alvo TLS só para baixar um jar é risco à toa nesta máquina.
# Antes do `COPY src` para que mudança de código não reinvalide os ~41 MB.
ARG NEWRELIC_AGENT_VERSION=9.4.0
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy \
        -Dartifact=com.newrelic.agent.java:newrelic-agent:${NEWRELIC_AGENT_VERSION}:jar \
        -DoutputDirectory=/build/newrelic -Dmdep.stripVersion=true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# ============================================================
# Stage 2 — Runtime
# JRE enxuto + usuário não-root. Recebe só o artefato pronto.
# ============================================================
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /deployments

# Usuário não-root (uid/gid 1001)
RUN groupadd --system --gid 1001 app \
 && useradd  --system --uid 1001 --gid app app

# Estrutura em camadas do Quarkus (lib/ muda menos → melhor reuso de cache)
COPY --from=build --chown=1001:app /build/target/quarkus-app/lib/     ./lib/
COPY --from=build --chown=1001:app /build/target/quarkus-app/*.jar    ./
COPY --from=build --chown=1001:app /build/target/quarkus-app/app/     ./app/
COPY --from=build --chown=1001:app /build/target/quarkus-app/quarkus/ ./quarkus/

# Mesmo diretório porque é ao lado do jar que o agente procura o newrelic.yml.
COPY --from=build --chown=1001:app /build/newrelic/newrelic-agent.jar ./newrelic/newrelic.jar
COPY --chown=1001:app             docker/newrelic/newrelic.yml        ./newrelic/newrelic.yml

USER 1001
EXPOSE 8080
ENV QUARKUS_HTTP_HOST=0.0.0.0

# HEALTHCHECK omitido de propósito: a base JRE não traz curl/wget e instalar
# só para isso é peso desnecessário. O readinessProbe do K8s (Etapa 2) — que
# bate em /carworkshop/v1/q/health/ready — é o que realmente importa.

# Exec-form (não string shell) é o que mantém o java como PID 1 e faz o TERM do k8s chegar nele.
ENTRYPOINT ["java", "-javaagent:/deployments/newrelic/newrelic.jar", "-jar", "/deployments/quarkus-run.jar"]
