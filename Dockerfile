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

USER 1001
EXPOSE 8080
ENV QUARKUS_HTTP_HOST=0.0.0.0

# HEALTHCHECK omitido de propósito: a base JRE não traz curl/wget e instalar
# só para isso é peso desnecessário. O readinessProbe do K8s (Etapa 2) — que
# bate em /carworkshop/v1/q/health/ready — é o que realmente importa.
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
