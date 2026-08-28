# Car Workshop API

REST API para gestão de oficina mecânica — **FIAP Tech Challenge · Fase 2**.

Construída com **Quarkus 3.26.3**, **Java 17**, **PostgreSQL 16** e autenticação **JWT**.
Na Fase 2 a aplicação foi **refatorada para Clean Architecture** e ganhou toda
a camada de infraestrutura: **containerização** (Dockerfile multi-stage), **manifestos Kubernetes**,
**Infraestrutura como Código** (Terraform) e **pipelines CI/CD** (GitHub Actions).

> 🎥 **Vídeo demo:** _(https://www.youtube.com/watch?v=ZhGXcg6D1YA)_
> 📮 **Collection da API:** a collection oficial é o **Swagger UI** embutido — suba a aplicação e
> acesse [`/carworkshop/v1/swagger-ui`](#documentação-da-api). Cada endpoint traz *Try it out*
> (payloads de exemplo + `curl` equivalente).

---

## Índice

- [Visão Geral](#visão-geral)
- [Pré-requisitos](#pré-requisitos)
- [Quick Start com Docker](#quick-start-com-docker-recomendado)
- [Modo de Desenvolvimento](#modo-de-desenvolvimento-hot-reload)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Autenticação JWT](#autenticação-jwt)
- [Endpoints Disponíveis](#endpoints-disponíveis)
- [Fluxo Completo (Exemplo Rápido)](#fluxo-completo-exemplo-rápido)
- [Documentação da API](#documentação-da-api)
- [Arquitetura](#arquitetura)
- [Banco de Dados](#banco-de-dados)
- [Deploy no EKS (AWS)](#deploy-no-eks-aws)
- [Testes](#testes)
- [Observabilidade](#observabilidade)
- [Qualidade e Segurança](#qualidade-e-segurança)
- [CI/CD](#cicd)
- [Melhorias Futuras](#melhorias-futuras)
- [Tecnologias](#tecnologias)

---

## Visão Geral

Sistema que digitaliza as operações de uma oficina mecânica de médio porte, substituindo planilhas e
processos manuais por uma API REST completa.

**Domínios cobertos:**
- Cadastro e gestão de clientes e veículos
- Catálogo de serviços e controle de estoque de peças
- Ordens de serviço com ciclo de vida completo
- Portal de acompanhamento público para o cliente final
- Autenticação JWT com controle de acesso por papel

**Ciclo de vida da Ordem de Serviço:**
```
RECEIVED → UNDER_DIAGNOSIS → PENDING_APPROVAL → IN_PROGRESS → COMPLETED → DELIVERED
                                     └──────────────────────────────────────► CANCELED
```
> `PENDING_APPROVAL → CANCELED` ocorre quando o cliente rejeita o orçamento. `RECEIVED →
> UNDER_DIAGNOSIS` é disparado por um agendador (`@Scheduled`); as demais transições são endpoints.

---

## Pré-requisitos

| Ferramenta     | Versão mínima | Observação                                              |
|----------------|---------------|--------------------------------------------------------|
| Docker         | 24+           | suficiente para rodar local (o build acontece na imagem)|
| Docker Compose | v2            | `docker compose ...`                                    |
| Java           | 17            | só para modo dev / rodar os testes fora de container   |
| Maven          | 3.9+          | ou use o wrapper `./mvnw` incluso no projeto           |
| jq             | qualquer      | usado para extrair o token JWT nos exemplos curl       |

**Para deploy em Kubernetes** (opcional): `kubectl` 1.32+ e a AWS CLI v2 configurada — necessários
**apenas** para a seção [Deploy no EKS (AWS)](#deploy-no-eks-aws), que também depende do cluster,
do registry e do banco já provisionados pelos repositórios de infraestrutura.

> **Windows:** substitua `./mvnw` por `mvnw.cmd` nos comandos de modo dev/testes.

---

## Quick Start com Docker (recomendado)

O `Dockerfile` é **multi-stage** — o Maven compila **dentro da imagem**. Não é preciso rodar
`./mvnw package` antes: `docker compose` cuida de tudo.

```bash
# 1. Clone o repositório
git clone https://github.com/CleytonOngaratto/FIAPchallenge.git
cd FIAPchallenge

# 2. Provisione o par RSA do JWT — a chave não vive mais no repositório (F9).
#    Copie o par de <raiz-do-projeto>/.secrets/ para ./secrets/ (gitignored):
mkdir -p secrets && cp ../.secrets/*.pem secrets/

# 3. Suba a stack completa (app + banco PostgreSQL)
docker compose up --build
```

O `docker compose` sobe o **banco** (`oficina_db`), espera ele ficar *healthy* (`pg_isready`) e só
então sobe a **aplicação** (`oficina_app`) — a app só fica `ready` depois que o Postgres responde.

Confirme que subiu (o `/health/ready` inclui o check do banco):

```bash
curl -s http://localhost:8080/carworkshop/v1/q/health/ready
# {"status":"UP", ...}
```

A API fica disponível em `http://localhost:8080/carworkshop/v1`.

> O Flyway aplica as migrations e insere dados de seed: **3 clientes**, **3 veículos**,
> **10 serviços** e **10 peças** prontos para uso.

**Portas:** app em `8080`; o banco é exposto no host em `5433` por padrão (configurável via
`DB_PORT_HOST` no `.env` — ver [.env.example](.env.example)). O SonarQube fica num profile separado
e **não** sobe no fluxo padrão:

```bash
docker compose --profile sonar up      # inclui o SonarQube em http://localhost:9000
```

---

## Modo de Desenvolvimento (hot reload)

```bash
# 1. Inicia apenas o banco (exposto no host em 5433, que é o default do datasource dev)
docker compose up -d oficina_db

# 2. Garanta que ./secrets/ tem o par RSA (mesmo passo do Quick Start)
mkdir -p secrets && cp ../.secrets/*.pem secrets/

# 3. Sobe a aplicação com live coding
./mvnw quarkus:dev
```

- API: `http://localhost:8080/carworkshop/v1`
- Dev UI: `http://localhost:8080/carworkshop/v1/q/dev/`
- Swagger UI: `http://localhost:8080/carworkshop/v1/swagger-ui`

---

## Variáveis de Ambiente

A aplicação consome estas variáveis (o Quarkus faz o *override* automático dos valores do
`application.properties`):

| Variável                          | Descrição                                    | Padrão (dev local)                              |
|-----------------------------------|----------------------------------------------|-------------------------------------------------|
| `SECRET_KEY`                      | Chave usada no hash de senhas (SHA-256 + salt)| `oficina-secret-key-dev`                        |
| `DB_HOST`                         | Host do banco — **só o host**, sem porta      | `localhost`                                     |
| `DB_PORT`                         | Porta do banco                                | `5433`                                          |
| `DB_NAME`                         | Nome do banco                                 | `oficina_db`                                    |
| `DB_USERNAME`                     | Usuário do banco                              | `postgres`                                      |
| `DB_PASSWORD`                     | Senha do banco                                | `postgres`                                      |
| `DB_SSLMODE`                      | `sslmode` da JDBC URL                         | `disable`                                       |
| `JWT_PUBLIC_KEY_LOCATION`         | Chave RSA **pública** (valida o token)        | `file:./secrets/publicKey.pem`                  |
| `JWT_PRIVATE_KEY_LOCATION`        | Chave RSA **privada** (assina o token)        | `file:./secrets/privateKey.pem`                 |

> **Por que `DB_HOST` e `DB_PORT` separados:** é exatamente o formato do contrato do RDS no SSM
> (`/fase3/rds/endpoint` é o host **sem** porta, e a porta vem em `/fase3/rds/port`). A JDBC URL é
> montada a partir deles no `application.properties`, então o deploy não precisa concatenar nada.

> **`DB_SSLMODE`:** o RDS sobe com `rds.force_ssl = 1`, então em nuvem o valor é **`require`** — o
> pgjdbc negociaria TLS sozinho com o default `prefer`, mas aqui a exigência fica explícita em vez de
> depender de um default de driver. No Postgres local do compose, `disable`.

> **Chave JWT (F9):** o par RSA **não** está no repositório nem dentro da imagem. Em dev ele vem de
> `./secrets/` (gitignored — ver Quick Start, passo 2); em produção vem do **SSM SecureString** para
> um **Secret do Kubernetes montado como volume**, e as duas variáveis acima apontam para o
> `mountPath`. A app precisa das **duas** chaves: ela valida o token do cliente (emitido pela Lambda)
> e assina o token de admin do `/auth/login`.

> No `docker compose` esses valores já são injetados (a app aponta para o service `oficina_db` na
> rede interna). No Kubernetes cada uma vem de um lugar diferente — ver [Deploy no EKS](#deploy-no-eks-aws):

| Variável | Origem no EKS |
|---|---|
| `QUARKUS_*`, `DB_SSLMODE`, `JWT_*_KEY_LOCATION` | ConfigMap `car-workshop-api-config` (versionado, não sensível) |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` | Secret `car-workshop-db`, gerado de `/fase3/rds/*` no deploy |
| `SECRET_KEY` | Secret `car-workshop-app`, de `/fase3/app/secret-key` (SecureString de bootstrap) |
| `NEW_RELIC_LICENSE_KEY` | Secret `car-workshop-app`, de `/fase3/newrelic/license-key` — opcional |
| chave RSA (os dois PEMs) | Secret `car-workshop-jwt` **montado como volume** em `/deployments/secrets`, de `/fase3/jwt/*` |

---

## Autenticação JWT

Endpoints administrativos exigem um token JWT no header `Authorization: Bearer <token>` e o papel
`ADMIN`. Os endpoints de acompanhamento (`/tracking/*`) exigem **`CUSTOMER` ou `ADMIN`**. Só
`/auth/*` é **público**. Tokens (RS256, SmallRye JWT) expiram em **1 hora**.

> **Token de cliente (F10):** quem emite não é esta app — é a **Lambda de autenticação por CPF**
> (repositório `fiap-fase3-auth-serverless`). Ela assina com a **mesma chave RSA**, o mesmo issuer
> (`https://oficina-api.com`), `groups=["CUSTOMER"]` e uma claim `cpf`. A app apenas **valida**.
> `/auth/login` continua existindo e emite o token de `ADMIN` da operação interna.

### Passo a passo

```bash
# 1. Criar conta admin (só na primeira vez)
curl -s -X POST http://localhost:8080/carworkshop/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123","roles":["ADMIN"]}'

# 2. Login — obter token e exportar para a sessão
export TOKEN=$(curl -s -X POST http://localhost:8080/carworkshop/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# 3. Usar nas chamadas
curl -s http://localhost:8080/carworkshop/v1/customers/get-all \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Endpoints Disponíveis

| Grupo                | Base Path              | Auth    | Descrição                                        |
|----------------------|------------------------|---------|--------------------------------------------------|
| Auth                 | `/auth`                | Público | Signup e login                                   |
| Clientes             | `/customers`           | ADMIN   | CRUD de clientes com validação de CPF            |
| Veículos             | `/vehicles`            | ADMIN   | CRUD de veículos com validação de placa          |
| Serviços             | `/service`             | ADMIN   | Catálogo de serviços com precificação            |
| Peças e Suprimentos  | `/parts-and-supplies`  | ADMIN   | Estoque de peças com controle de quantidade      |
| Ordens de Serviço    | `/work-orders`         | ADMIN   | Criação e gestão do ciclo de vida completo       |
| Acompanhamento       | `/tracking`            | CUSTOMER ou ADMIN | Portal do cliente: consulta e aprovação/rejeição |

Para **exemplos completos de cada endpoint** (payloads, parâmetros e respostas), use o **Swagger UI**
(`/carworkshop/v1/swagger-ui`): o botão **Try it out** executa a chamada real e mostra o `curl`
equivalente. É a nossa collection oficial — ver [Documentação da API](#documentação-da-api).

---

## Fluxo Completo (Exemplo Rápido)

```bash
# Após exportar o TOKEN (ver seção Autenticação acima)

# Consultar clientes já criados pelo seed
curl -s http://localhost:8080/carworkshop/v1/customers/get-all \
  -H "Authorization: Bearer $TOKEN" | jq .

# Criar uma ordem de serviço para o cliente 1, veículo 1, com o serviço 1
curl -s -X POST http://localhost:8080/carworkshop/v1/work-orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"customer_id":1,"vehicle_id":1,"service_ids":[1]}' | jq .

# O cliente acompanha a OS com o token que a Lambda emitiu a partir do CPF dele
# (em dev, um token de ADMIN também abre a rota)
curl -s http://localhost:8080/carworkshop/v1/tracking/1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Documentação da API

A **API é autodocumentada via OpenAPI/Swagger** — esta é a **collection oficial** do projeto (não há
Postman/OpenAPI exportado à parte). Suba a aplicação e acesse:

| Recurso       | URL                                                   |
|---------------|-------------------------------------------------------|
| Swagger UI    | `http://localhost:8080/carworkshop/v1/swagger-ui`     |
| OpenAPI JSON  | `http://localhost:8080/carworkshop/v1/q/openapi`      |

No Swagger UI, o **Try it out** de cada endpoint executa a chamada e exibe o `curl` correspondente.

---

## Arquitetura

A aplicação combina **Clean Architecture** (camadas concêntricas — dependências só apontam para
dentro) com **Monólito Modular** (bounded contexts isolados por pacote, prontos para extração em
microsserviços). O **núcleo (`domain` + `usecase`) é Java puro** — sem `jakarta.*`/`io.quarkus.*`;
o framework mora só nas camadas externas. As regras abaixo são **impostas por testes** (ArchUnit).

### Visão C4 (Contexto e Container)

Diagramas C4 do sistema (fonte PlantUML em [`docs/c4model/`](docs/c4model)). As imagens abaixo já
estão renderizadas — não é preciso IntelliJ nem plugin para vê-las.

**Nível 1 — Contexto** — atores e o sistema como caixa-preta:

![Diagrama de Contexto (C4 Nível 1)](docs/c4model/Diagrama_de_Contexto.png)

**Nível 2 — Container** — a API (Quarkus) e o banco de dados (PostgreSQL):

![Diagrama de Container (C4 Nível 2)](docs/c4model/Diagrama_de_Container.png)

### Camadas (a dependência só aponta para dentro)

```text
┌─────────────────────────────────────────┐
│              Infrastructure              │  ← Quarkus, JPA/Panache, JWT, CDI, producers
│  ┌───────────────────────────────────┐  │
│  │             Adapter               │  │  ← Controllers, Gateways, DTOs, Presenters
│  │  ┌─────────────────────────────┐ │  │
│  │  │          Use Case           │ │  │  ← Interactors (Java puro), Ports (interfaces)
│  │  │  ┌───────────────────────┐  │ │  │
│  │  │  │        Domain         │  │ │  │  ← Entities, Validators, Value Objects (Java puro)
│  │  │  └───────────────────────┘  │ │  │
│  │  └─────────────────────────────┘ │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘

Regra de ouro: nenhuma camada interna importa uma camada mais externa.
```

| Camada             | Responsabilidade                                                        | Framework? |
|--------------------|-------------------------------------------------------------------------|------------|
| **Domain**         | Entidades, regras de negócio (state machine, invariantes), validadores  | ❌ Java puro |
| **Use Case**       | Interactors que orquestram o domínio; injeção por **construtor**; Ports (in/out) | ❌ Java puro |
| **Adapter**        | Controllers (REST), Gateways (impl. dos ports), DTOs, Presenters        | ✅ `@Inject`/`@ApplicationScoped` |
| **Infrastructure** | Producers CDI (`@Produces` por módulo), JPA/Panache, MapStruct          | ✅ Quarkus |

A amarração CDI do interactor mora num **producer por módulo** (`infrastructure/config/XModuleConfig`),
mantendo o `usecase` livre do framework.

### Bounded Contexts (7 pacotes)

`workorder` é o **hub**: fala com os outros contextos **apenas pelos ports públicos** deles (via
data gateways), nunca pelo Panache alheio.

```text
workorder  ── hub das Ordens de Serviço. Consome os outros contextos SEMPRE pelo
   │           port público de cada um (via data gateways) — nunca o Panache alheio.
   ├──▶ customer       (port público)
   ├──▶ vehicle        (port público)
   ├──▶ autoservice    (port público)
   └──▶ inventory      (port público)

vehicle  ──▶ customer   valida se o dono existe, também via port público.

shared   ◀── usado por  auth · customer · vehicle · autoservice · inventory · workorder
             infra transversal: exceções, error response e mappers HTTP.
```

Estrutura interna de cada módulo:

```
{modulo}/
├── domain/           ← entity/ (regras de negócio) + validator/   [Java puro]
├── usecase/          ← port/in/ · port/out/ · interactor/          [Java puro]
├── adapter/          ← controller/ · dto/ · presenter/ · gateway/
└── infrastructure/   ← config/ (producer CDI) · persistence/ (JPA) · mapper/ (MapStruct)
```

### Modelo de Domínio (estado desacoplado — Fase 2)

Os contextos foram **desacoplados** para viabilizar a extração em microsserviços:

- **Cliente** é um agregado próprio — não conhece `vehicle` (sem `@OneToMany`); pode existir sem veículo.
- **Veículo** referencia o dono por `customerId` (`Long`), **sem FK cruzada** (a migration `V3.0.0`
  dropou a FK). Todo veículo **exige** um `customerId` válido (validado via port público do `customer`):
  ausente → HTTP 400; cliente inexistente → HTTP 404.
- **Ordem de Serviço** guarda `customer_id`/`vehicle_id` (`Long`) + **snapshots** (`customer_email`,
  `vehicle_plate`, e serviços/peças com **nome e preço da época**). Preserva o histórico mesmo que
  cliente/serviço/peça mudem depois — trade-off: perde-se o *live join*, ganha-se desacoplamento.

---

## Banco de Dados

### Justificativa da escolha: PostgreSQL 16

**Por que banco relacional?**
O domínio da oficina é altamente relacional: uma Ordem de Serviço referencia um Cliente, um Veículo,
múltiplos Serviços e múltiplas Peças. Um banco relacional garante integridade e transações ACID,
eliminando inconsistências difíceis de controlar em nível de aplicação.

**Por que PostgreSQL e não MySQL ou MariaDB?**
PostgreSQL tem conformidade mais rigorosa com o padrão SQL, MVCC (Multiversion Concurrency Control)
para transações concorrentes e tipagem mais rica. Para um controle de estoque com leituras e
atualizações simultâneas, o MVCC reduz contenção de locks. É também o banco suportado nativamente
pelo ecossistema Quarkus + Hibernate Panache.

**Por que não NoSQL (MongoDB, DynamoDB)?**
Os dados têm esquema bem definido e relacionamentos N:M (OS ↔ Serviços, OS ↔ Peças). A consistência
eventual típica de NoSQL não é adequada a um sistema transacional onde o controle de estoque e a
progressão do status de uma OS precisam ser atômicos.

**Migrations (Flyway):**

| Versão | Descrição |
|--------|-----------|
| V1.0.0 | Schema completo: tabelas, sequences, dados de seed |
| V2.0.0 | `work_orders.deleted` (soft-delete) |
| V3.0.0 | Desacopla `vehicle`↔`customer` (dropa a FK cruzada; `owner_id` vira `customerId Long`) |
| V4.0.0 | `work_orders` com `customer_id`/`vehicle_id` (`Long`) + snapshots; dropa FKs cruzadas |

| Ambiente   | Banco         | URL                                                |
|------------|---------------|----------------------------------------------------|
| Dev local  | PostgreSQL 16 | `jdbc:postgresql://localhost:5433/oficina_db`      |
| Testes     | H2 in-memory  | schema `create-drop` + seed em `import-test.sql`   |
| Kubernetes | PostgreSQL 16 | `jdbc:postgresql://postgres.car-workshop.svc.cluster.local:5432/oficina_db` |

---

## Deploy no EKS (AWS)

Manifestos em [`k8s/application/`](k8s/application), deploy em [`scripts/deploy.ps1`](scripts/deploy.ps1).
O cluster (EKS), o registry (ECR) e o banco (RDS) vêm dos repositórios de infraestrutura — este repo
só publica a imagem e aplica os manifestos.

| Manifesto | O que traz |
|---|---|
| `namespace.yaml` | Namespace `car-workshop` |
| `configmap.yaml` | Config não sensível: root-path, Flyway, `DB_SSLMODE=require`, caminho das chaves JWT |
| `deployment.yaml` | 2 réplicas, imagem do ECR, probes, `requests`/`limits`, volume do JWT |
| `service.yaml` | `type: LoadBalancer` — ELB público, `:80` → `:8080` |
| `hpa.yaml` | `autoscaling/v2`, min 2 / max 10, CPU 60% + memória 80% |

**Não existe `secret.yaml`.** Nenhum segredo é versionado: os três Secrets são gerados pelo
`deploy.ps1` a partir do **SSM Parameter Store**, no momento do deploy.

| Secret | Chaves | Origem no SSM |
|---|---|---|
| `car-workshop-db` | `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` | `/fase3/rds/*` |
| `car-workshop-app` | `SECRET_KEY`, `NEW_RELIC_LICENSE_KEY` | `/fase3/app/secret-key`, `/fase3/newrelic/license-key` |
| `car-workshop-jwt` | `privateKey.pem` `publicKey.pem` — montados em `/deployments/secrets` | `/fase3/jwt/*` |

> **Por que regerar sempre:** a senha do RDS é recriada a cada `destroy` + `apply` da infraestrutura
> de banco. Um Secret criado "uma vez" faz a app subir e **falhar ao conectar** com a credencial da
> sessão anterior — sintoma que parece problema de rede.

> **Por que a chave JWT é volume e não env var:** em env var ela apareceria no `kubectl describe
> pod`, seria herdada por todo processo filho e entraria na captura de ambiente do agente New Relic.

> A license key do New Relic é **opcional**: sem ela a app sobe normalmente, apenas sem APM.

### Pré-requisitos

1. EKS, ECR e RDS provisionados, com o contrato publicado em `/fase3/*`.
2. `/fase3/app/secret-key` publicado **uma vez** (parâmetro de bootstrap — nenhum Terraform o cria):

   ```powershell
   aws ssm put-parameter --name /fase3/app/secret-key --type SecureString --region us-east-1 `
     --value ([Convert]::ToBase64String([byte[]](1..32 | ForEach-Object { Get-Random -Maximum 256 })))
   ```

   ⚠️ **Publique e não rotacione.** É o *pepper* do hash de senha (`SECRET_KEY`): trocá-lo faz todo
   admin já cadastrado parar de logar, com erro que parece senha errada.
3. `kubectl` apontando para o cluster:

   ```powershell
   $cluster = aws ssm get-parameter --name /fase3/eks/cluster-name --query Parameter.Value --output text
   aws eks update-kubeconfig --name $cluster --region us-east-1
   ```

### Deploy

```powershell
# 1. Imagem no ECR — TODA sessão, não uma vez: o repositório é criado com `force_delete`, então o
#    `terraform destroy` da infraestrutura leva as imagens junto.
$tag = git rev-parse --short HEAD
$ecr = aws ssm get-parameter --name /fase3/ecr/repo-url --query Parameter.Value --output text
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ecr.Split('/')[0]
docker build -t "${ecr}:${tag}" .
docker push  "${ecr}:${tag}"

# 2. Deploy — lê o SSM, gera os Secrets e aplica os manifestos na ordem certa
.\scripts\deploy.ps1 -Tag $tag
```

> ⚠️ **`kubectl apply -f k8s/application/` sozinho não funciona**, por dois motivos: o
> `deployment.yaml` traz o placeholder `__IMAGE__` (a URL do ECR carrega o account id e muda por
> sessão do lab, então não pode ser versionada), e o diretório é aplicado em **ordem alfabética**,
> com `namespace.yaml` depois dos objetos que dependem dele. O `deploy.ps1` resolve os dois.

> ⚠️ **Tag imutável, nunca `latest`.** Com tag fixa o *spec* do Deployment não muda, o `apply` vira
> no-op e o `rollout status` responde *"successfully rolled out"* na hora — validando a imagem
> **antiga**. Para republicar sob a mesma tag: `.\scripts\deploy.ps1 -Restart`.

### Verificação

```powershell
kubectl -n car-workshop get pods,svc,hpa
kubectl -n car-workshop get deploy car-workshop-api -o jsonpath='{.spec.template.spec.containers[0].image}'

$lb = kubectl -n car-workshop get svc car-workshop-api -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
curl.exe -i "http://$lb/carworkshop/v1/q/health/ready"      # 200 {"status":"UP"}
```

> **`curl.exe`, não `curl`:** no PowerShell 5.1 `curl` é alias de `Invoke-WebRequest`, que sem
> `-UseBasicParsing` quebra em terminal não interativo. E o DNS do ELB leva ~3 min para propagar
> depois de o hostname aparecer — antes disso a falha é de DNS, não da app.

A chave privada **não** pode estar no ambiente do processo. `kubectl describe pod` não serve como
prova: ele nunca imprime valor de Secret, então passaria mesmo se a chave estivesse em env var.

```powershell
$pod = kubectl -n car-workshop get pod -l app=car-workshop-api -o jsonpath='{.items[0].metadata.name}'
kubectl -n car-workshop exec $pod -- ls -l /deployments/secrets     # os 2 PEMs
kubectl -n car-workshop exec $pod -- sh -c 'env | grep -c BEGIN'    # 0
```

Agente New Relic conectado — o `--tail=-1` é **obrigatório**: com seletor o `kubectl logs` mostra só
10 linhas, e a mensagem sai no boot.

```powershell
kubectl -n car-workshop logs -l app=car-workshop-api --tail=-1 | Select-String "connected to collector"
```

Migrations aplicadas **no banco** (não só no log). O pod lê tudo do Secret que o deploy gerou, o que
de quebra prova que a credencial da app está correta:

```powershell
@'
apiVersion: v1
kind: Pod
metadata: { name: psql, namespace: car-workshop }
spec:
  restartPolicy: Never
  containers:
    - name: psql
      image: postgres:16
      command: ["psql", "-c", "select version, description, success from flyway_schema_history order by installed_rank;"]
      env:
        - { name: PGHOST,     valueFrom: { secretKeyRef: { name: car-workshop-db, key: DB_HOST     } } }
        - { name: PGPORT,     valueFrom: { secretKeyRef: { name: car-workshop-db, key: DB_PORT     } } }
        - { name: PGDATABASE, valueFrom: { secretKeyRef: { name: car-workshop-db, key: DB_NAME     } } }
        - { name: PGUSER,     valueFrom: { secretKeyRef: { name: car-workshop-db, key: DB_USERNAME } } }
        - { name: PGPASSWORD, valueFrom: { secretKeyRef: { name: car-workshop-db, key: DB_PASSWORD } } }
'@ | kubectl apply -f -
kubectl -n car-workshop wait --for=jsonpath='{.status.phase}'=Succeeded pod/psql --timeout=120s
kubectl -n car-workshop logs psql
kubectl -n car-workshop delete pod psql --now
```

> A senha vai por `secretKeyRef`, e não em `--env=` na linha de comando: ela é gerada com
> `*()-_=+[]{}` no alfabeto, e passar isso como argumento nativo no PowerShell 5.1 mangla o valor —
> a falha apareceria como "erro de autenticação", mandando caçar security group.

### Demo de autoescala (HPA por CPU)

O `metrics-server` vem como addon do cluster; sem ele as métricas ficam `<unknown>` e nada escala.

```powershell
kubectl -n car-workshop run load --image=busybox --restart=Never -- `
  /bin/sh -c "while true; do wget -qO- http://car-workshop-api.car-workshop.svc.cluster.local/carworkshop/v1/q/health/live; done"

kubectl -n car-workshop get hpa -w     # réplicas sobem conforme a CPU
```

### Encerrando a sessão

```powershell
kubectl -n car-workshop delete svc car-workshop-api
```

⚠️ **Antes de destruir a infraestrutura**, apague todo Service `LoadBalancer`: o ELB deixa uma ENI na
subnet e **trava a destruição da VPC**.
---

## Testes

```bash
# Roda todos os testes (H2 em memória, sem banco externo necessário)
./mvnw clean verify

# Roda uma classe específica
./mvnw test -Dtest=WorkOrderInteractorTest
```

- **Suíte:** **404 testes** automatizados, todos verdes (`./mvnw clean verify` → `BUILD SUCCESS`).
- **Tipos:** unidade (Mockito), HTTP real (REST-Assured) e **arquitetura** (ArchUnit — trava as
  regras da Clean Architecture; uma violação nova quebra o build).
- **Cobertura:** gate **de 75%** (BUNDLE/INSTRUCTION) via JaCoCo, configurado no `pom.xml` — o
  desafio não fixa um percentual mínimo; o piso é um alvo interno de qualidade.
- **Porta de testes:** 9090.

O relatório de cobertura HTML é gerado em `target/site/jacoco/index.html` após `./mvnw clean verify`.

---

## Observabilidade

A instrumentação vive na aplicação; os dashboards e alertas são montados no **New Relic**, que
consome as três saídas abaixo.

| Saída | Onde | O que entrega |
|---|---|---|
| **Logs JSON** | stdout (`quarkus-logging-json`) | Cada evento sai como um objeto JSON com `service.name`, nível, logger, stack trace e o **MDC** — ingerível direto, sem parser de texto. |
| **Métricas** | `GET /carworkshop/v1/q/metrics` | Formato Prometheus via Micrometer: JVM (heap, GC, threads), HTTP server (latência e contagem por rota/status) e as métricas de negócio abaixo. |
| **Correlação** | header `X-Trace-Id` | Um `traceId` por requisição, no MDC de todas as linhas de log daquela requisição. |

**Correlação de requisições.** [`TraceIdFilter`](src/main/java/br/com/fiap/postech/carworkshop/shared/infrastructure/observability/TraceIdFilter.java)
põe um `traceId` no MDC no início de cada requisição e o devolve no header `X-Trace-Id`. Se o
chamador já mandou um `X-Trace-Id`, ele é **reaproveitado** em vez de substituído — é isso que
mantém a correlação de ponta a ponta quando a chamada vem pelo API Gateway/Lambda, em vez de a
trilha recomeçar na borda do cluster. O MDC é limpo na resposta: as threads são reusadas do pool e
um resto de MDC carimbaria a requisição seguinte com o id da anterior.

```bash
# O header sai em rotas JAX-RS. /q/health/ready é servido pelo SmallRye Health, que não passa
# pelos filtros de request/response — ali o X-Trace-Id não aparece (e não deveria).
curl -si http://localhost:8080/carworkshop/v1/customers/get-all \
  -H "Authorization: Bearer $TOKEN" | grep -i x-trace-id

# Mandando um id de entrada, ele é reaproveitado em vez de substituído:
curl -si http://localhost:8080/carworkshop/v1/customers/get-all \
  -H "Authorization: Bearer $TOKEN" -H "X-Trace-Id: meu-id-123" | grep -i x-trace-id

curl -s  http://localhost:8080/carworkshop/v1/q/metrics | grep workorder
```

**Métricas de negócio.** Derivadas das transições de status que os casos de uso já executam —
**nenhuma migration nova** foi necessária:

| Métrica | Tipo | Tags | Alimenta |
|---|---|---|---|
| `workorder.status.changes` | counter | `status` | Volume diário de OS e distribuição entre Diagnóstico / Execução / Finalização |
| `workorder.completion.time` | timer | — | Tempo médio de atendimento (criação → conclusão) |

O caminho é `WorkOrderInteractor` → `WorkOrderMetricsPort` (porta de saída, Java puro) →
`WorkOrderMetricsAdapter` (Micrometer). A porta existe para que o caso de uso não conheça o
Micrometer: a regra de arquitetura `usecase ∌ infrastructure` é **estrita** e quebra o build se for
violada — ver [Arquitetura](#arquitetura).

> Em dev e nos testes o log JSON fica **desligado** (`%dev`/`%test`), porque no terminal ele só
> atrapalha a leitura. Em container ele é o default.

### New Relic APM (agente Java)

As três saídas acima são geradas pela aplicação. Quem as transforma em APM — transações, latência por
rota, erros, distributed tracing — é o **agente Java do New Relic**, que só existe dentro da imagem.

**Como ele entra.** O `Dockerfile` baixa `com.newrelic.agent.java:newrelic-agent` (versão pinada no
`ARG NEWRELIC_AGENT_VERSION`) do **Maven Central**, no stage de build, e copia para o runtime. Não é
capricho: o stage de runtime (`eclipse-temurin:17-jre-jammy`) não tem `curl` nem `wget`, e o build já
fala com o Maven Central — puxar de outro host seria acrescentar um endpoint TLS a mais para dar
errado. O `-javaagent` entra como elemento do array do `ENTRYPOINT`, em exec-form, para que o `java`
continue sendo o PID 1 e o `SIGTERM` do Kubernetes chegue nele.

**A license key nunca entra na imagem.** Ela vive em `/fase3/newrelic/license-key` (SSM
SecureString) e chega por `NEW_RELIC_LICENSE_KEY` em runtime — Secret do Kubernetes no cluster,
variável de ambiente no compose. **Sem a chave a aplicação sobe normalmente**: o agente carrega, loga
`license_key is empty in the config. Not starting New Relic Agent.` e sai do caminho. Para desligar o
agente por completo, `NEW_RELIC_AGENT_ENABLED=false`.

```powershell
# Local, sem gravar a chave em disco (o .env seria uma terceira cópia dela):
$env:NEW_RELIC_LICENSE_KEY = (aws ssm get-parameter --name /fase3/newrelic/license-key `
  --with-decryption --query Parameter.Value --output text --region us-east-1)
# --build é obrigatório: o serviço builda do Dockerfile, e --force-recreate sozinho recria o
# container a partir da imagem VELHA — subiria sem o agente.
docker compose up -d --build --force-recreate oficina_app
docker compose logs oficina_app | Select-String 'connected to collector'
```

**Logs-in-context.** A configuração está em [`docker/newrelic/newrelic.yml`](docker/newrelic/newrelic.yml),
versionada e sem segredo. O agente **envia os logs ele mesmo** (`application_logging.forwarding`) e o
**decorator fica desligado**. O `newrelic.yml` de referência do agente **recomenda** não usar os dois
juntos (não impede); o argumento decisivo é outro — o decorator anexa o carimbo `NR-LINKING|…`
*depois* da linha já formatada, o que cairia após o `}` do nosso log JSON e quebraria qualquer parser
adiante.

> ⚠️ **O stdout do container é de formato misto.** A aplicação escreve JSON; o agente escreve o
> próprio log em texto puro no mesmo stream (`log_file_name: STDOUT`, para não exigir um diretório
> gravável pelo uid 1001 nem esconder o diagnóstico de quem roda `kubectl logs`). Consequência
> prática: `kubectl logs … | jq` engasga nas linhas do agente. Filtre antes —
> `kubectl logs … | grep '^{' | jq`.

Cada log forwardado chega ao New Relic com **dois** identificadores de correlação, verificados no
payload real (`NEW_RELIC_AUDIT_MODE=true`, que loga o JSON enviado ao coletor):

| Campo | Origem | Serve para |
|---|---|---|
| `trace.id` / `span.id` | o agente, ao serializar o payload | o pulo nativo do New Relic entre APM e Log |
| `context.traceId` | o MDC do `TraceIdFilter` (4a) | seguir o id que **entrou pelo header** `X-Trace-Id` |

Os dois importam por motivos diferentes. O `trace.id` é interno do New Relic e nasce na app. O
`context.traceId` é o único que atravessa a borda: quando o Bloco 5 puser o API Gateway e a Lambda
na frente, é ele que amarra a requisição inteira. Por isso o `forwarding.context_data` está
**ligado**, com allowlist `include: traceId` — sem ele o MDC não sobe e esse elo se perde. A
allowlist é explícita de propósito: se alguém puser outra coisa no MDC amanhã, ela não vaza para o
New Relic sem uma decisão.

A label `project: fiap-fase3` acompanha cada log como **`tags.project`** (é o que
`forwarding.labels.enabled` faz; no default ela ficaria só na entidade APM).

No painel:

```sql
-- pelo id que entrou na borda
SELECT * FROM Log WHERE context.traceId = '<id do header X-Trace-Id>'
-- ou pelo trace do New Relic, que também liga direto da transação no APM
SELECT * FROM Log WHERE trace.id = '<trace>'
```

> ⚠️ Os **~118 primeiros logs do startup não chegam** ao New Relic: são emitidos antes de o
> `LogSenderService` do agente subir, e o agente os descarta registrando em `FINER` — invisível em
> produção. Para o log de arranque, use `kubectl logs` do pod.

> ⚠️ **Herança para o Bloco 4e.** O `nri-bundle` traz um Fluent Bit que despacha o stdout dos pods.
> Com o agente já forwardando, os dois juntos entregam **cada linha duas vezes** — o bundle deve ser
> instalado com o componente `newrelic-logging` desligado. O custo é perder o log dos pods não
> instrumentados (kube-system, CoreDNS); aceitável aqui, porque a aplicação é a única carga do
> cluster e ela reporta sozinha.

---

## Qualidade e Segurança

Duas frentes cuidam da saúde do código e da segurança da aplicação: **análise estática** (SonarQube)
e **scan de vulnerabilidades** (Trivy). Ambos rodam localmente e no CI.

### SonarQube (análise estática)

Instância local do SonarQube Community Edition, num profile separado do compose:

```bash
# 1. Suba o container do SonarQube
docker compose --profile sonar up -d sonarqube

# 2. Aguarde inicializar (~60s) e acesse http://localhost:9000 (login padrão admin/admin)

# 3. Execute a análise (substitua <sua_senha> pela senha definida)
./mvnw clean verify sonar:sonar \
  "-Dsonar.host.url=http://localhost:9000" \
  "-Dsonar.login=admin" \
  "-Dsonar.password=<sua_senha>"
```

> **Nota Windows (PowerShell):** use aspas nos parâmetros `-D` para evitar problemas com o parser.

![Relatório do SonarQube — Quality Gate](docs/img/sonarqube.png)

### Trivy (scan de vulnerabilidades)

O CI roda o **Trivy** pela imagem oficial fixada `aquasec/trivy:0.58.0` (reprodutível; o banco de
vulnerabilidades é baixado fresco a cada scan). Cobre `CRITICAL`, `HIGH` e `MEDIUM`; é **advisory**
(um achado não derruba o build) e publica um relatório HTML como *artifact* da pipeline (retido 30
dias em **Actions → execução → Artifacts**). Para rodar o mesmo scan localmente:

```bash
docker run --rm -v "${PWD}:/workspace" aquasec/trivy:0.58.0 \
  fs /workspace --severity CRITICAL,HIGH,MEDIUM
```

![Relatório de vulnerabilidades do Trivy](docs/img/trivy.png)

### Boas práticas já aplicadas

- **Zero credencial hardcoded** nos workflows e no Terraform (tudo via GitHub Secrets / variáveis sem
  default; `terraform.tfvars` no `.gitignore`).
- Imagem roda **não-root** (uid 1001).
- **Nenhuma chave RSA no repositório nem dentro da imagem.** O par do JWT era commitado em
  `src/main/resources/` e ia assado no classpath; agora vem de `./secrets/` (gitignored) em dev e de
  um **Secret do Kubernetes montado como volume** em produção, alimentado pelo **SSM SecureString**.
  O par que estava commitado foi **aposentado** — o par de produção e o par de teste são novos e
  distintos entre si.
  > O par antigo continua no histórico do git e deve ser tratado como comprometido. O que o
  > neutraliza não é apagá-lo do histórico, e sim nada mais confiar nele.
- O par RSA em `src/test/resources/` é uma **fixture de teste** descartável, versionada de propósito:
  os testes assinam tokens offline com ela e ela nunca protege nada em execução real.
- **Nenhum segredo é versionado.** `k8s/application/` não tem `secret.yaml`: os três Secrets
  (`car-workshop-db`, `car-workshop-app`, `car-workshop-jwt`) são gerados do **SSM SecureString** no
  momento do deploy, e a credencial do banco é **regerada a cada deploy** porque a senha do RDS muda
  a cada recriação da infraestrutura.

---

## CI/CD

GitHub Actions cobre o fluxo completo exigido pela Fase 2:

| Workflow                                                 | Gatilho                     | O que faz |
|----------------------------------------------------------|-----------------------------|-----------|
| [`maven-ci.yml`](.github/workflows/maven-ci.yml)         | push/PR na `main`           | `mvn clean verify` (build + testes + gate de cobertura) e **scan de vulnerabilidades** com Trivy |
| [`cd.yml`](.github/workflows/cd.yml)                     | manual / push/PR na `main`  | build → imagem → sobe **Minikube efêmero** no runner → `terraform apply` (banco + app) → `rollout` + **smoke test** do `/health/ready` |

**Análise de vulnerabilidades (Trivy):** roda no `maven-ci` e publica um relatório HTML como
*artifact* da pipeline. Detalhes da configuração e como reproduzir local em
[Qualidade e Segurança](#qualidade-e-segurança).

> 🔴 **O `cd.yml` está desatualizado e falhando.** Ele é herança da Fase 2: sobe um Minikube efêmero
> e chama `terraform apply` em `infra/terraform/environments/minikube`, pasta que **não existe mais**
> neste repositório. O deploy real hoje é o do EKS, por `scripts/deploy.ps1` — este workflow será
> substituído por build → push no ECR → `kubectl apply`. Nenhum *status check* obrigatório depende
> dele, então o merge não é bloqueado.

---

## Melhorias Futuras

**Pipeline de CD para o EKS:** o deploy no EKS já está descrito em
[Deploy no EKS (AWS)](#deploy-no-eks-aws) e é feito por `scripts/deploy.ps1`. Falta automatizá-lo no
GitHub Actions (build → push no ECR → `kubectl apply` → `rollout status`), substituindo o `cd.yml`
de Minikube — ver o aviso na seção [CI/CD](#cicd).

> As boas práticas de segurança **já aplicadas** (não-root, zero credencial hardcoded, Secrets)
> estão em [Qualidade e Segurança](#qualidade-e-segurança).

---

## Tecnologias

| Tecnologia                  | Versão   | Uso                                           |
|-----------------------------|----------|-----------------------------------------------|
| Quarkus                     | 3.26.3   | Framework principal (REST, DI, config, ORM)   |
| Java                        | 17       | Linguagem                                     |
| PostgreSQL                  | 16       | Banco de dados de produção                    |
| Hibernate ORM Panache       | —        | Acesso a dados (`PanacheRepository`)          |
| Flyway                      | —        | Migrations versionadas de banco               |
| SmallRye JWT                | —        | Autenticação JWT (RS256)                      |
| MapStruct                   | 1.5.5    | Mapeamento Domain ↔ JPA Entity                |
| Lombok                      | 1.18.30  | Redução de boilerplate (compile-time)         |
| SmallRye OpenAPI            | —        | Documentação Swagger automática               |
| Quarkus Logging JSON        | —        | Logs estruturados p/ ingestão no New Relic    |
| Micrometer + Prometheus     | —        | Métricas JVM, HTTP e de negócio (`/q/metrics`)|
| New Relic Java Agent        | 9.4.0    | APM, distributed tracing e envio de logs      |
| JUnit 5 · Mockito · REST-Assured · ArchUnit | — | Testes: unidade, HTTP e arquitetura        |
| JaCoCo                      | 0.8.12   | Cobertura de testes (gate de 75% no `pom.xml`)|
| Docker + Docker Compose     | —        | Containerização (Dockerfile multi-stage)      |
| Kubernetes (Amazon EKS)     | —        | Orquestração e deploy (Deployment, Service LB, HPA) |
| AWS ECR · RDS PostgreSQL · SSM Parameter Store | — | Registry da imagem, banco gerenciado e contrato de configuração/segredos |
| GitHub Actions              | —        | CI/CD (build, testes, scan, deploy)           |
| Trivy (Aqua Security)       | 0.58.0   | Scan de vulnerabilidades no CI                |
| SonarQube Community Edition | 10.4.1   | Análise estática de qualidade                 |
