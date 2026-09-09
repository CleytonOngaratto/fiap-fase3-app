# RFC-001 — Nuvem AWS: rede, cluster, autoescala, repositórios e CI/CD

| | |
|---|---|
| **Status** | Implementada e verificada na AWS (última verificação completa: 2026-09-05) |
| **Data** | 2026-07-28 (decisão) · 2026-09-05 (revisão) |
| **Repositórios** | `fiap-fase3-infra-k8s` (rede, EKS, ECR), `fiap-fase3-app` (manifestos, HPA, CI/CD), com efeito nos outros dois |

## Motivação

O Tech Challenge pede que a oficina passe a operar em nuvem com Kubernetes gerenciado, banco
gerenciado, função serverless para autenticação, infraestrutura como código e CI/CD por repositório.
A nuvem disponível é o **AWS Academy Learner Lab**, e é ele — mais que qualquer preferência
técnica — que molda o desenho.

## Restrições do Learner Lab que moldam tudo

| Restrição | Consequência no desenho |
|---|---|
| **`iam:CreateRole` bloqueado.** Nenhum repositório cria role, user ou instance profile | Toda role vem por `data source`. São **três identidades**: `LabEksClusterRole` (control plane), `LabEksNodeRole` (nós) e `LabRole` (Lambda). As duas do EKS têm prefixo e sufixo gerados por CloudFormation, únicos por instância do lab e trocados a cada reset — por isso são variáveis sem default, descobertas por `scripts/preflight.ps1` |
| Credenciais temporárias de ~4 h | GitHub Secrets recolados a cada sessão; todo workflow tem uma guarda de "sessão expirada" com mensagem acionável |
| Crédito fixo de US$ 50, painel de custo com 8–12 h de atraso | **Infraestrutura destruída entre sessões.** O control plane do EKS fatura mesmo com o lab desligado: um teardown interrompido em 2026-09-02 deixou o cluster 23,7 h ligado e custou ~US$ 7, o equivalente a 17 ciclos completos de subir e derrubar |
| Máximo de 9 instâncias EC2 / 32 vCPU; 20 ou mais **desativam a conta** | `node_max_size = 4`; sem cluster-autoscaler |
| Só instâncias nano a large, on-demand; RDS só gp2, sem enhanced monitoring; Lambda com 10 execuções concorrentes | Dimensionamento fixo em `t3.medium`, `db.t3.micro`, sem `reserved_concurrent_executions` |
| Sem Route53/ACM garantidos | URLs default (`execute-api`, DNS do ELB), sem TLS customizado na borda |

## Proposta

### Quatro repositórios, um contrato

| # | Repositório | Provisiona | Por que separado |
|---|---|---|---|
| 2 | `fiap-fase3-infra-k8s` | VPC, EKS, node group, ECR | Ciclo de vida mais lento e blast radius maior: só o node group leva ~10 min para criar, e recriar o cluster derruba tudo |
| 3 | `fiap-fase3-infra-db` | RDS, subnet group, security groups | Dado: o banco pode ser recriado sem tocar no cluster, e vice-versa |
| 4 | `fiap-fase3-app` | Imagem, Deployment, Service, HPA, New Relic | Muda a cada feature; é o único com pipeline de aplicação |
| 1 | `fiap-fase3-auth-serverless` | Lambda, API Gateway | Borda: depende de todos os outros e é o último a subir |

A comunicação entre eles é **só por SSM Parameter Store** (`/fase3/*`): cada repositório publica o
que cria e lê o que precisa por `data "aws_ssm_parameter"`. Ordem de deploy **2 → 3 → 4 → 1**, de
destroy **1 → 4 → 3 → 2**. O porquê do SSM em vez de `terraform_remote_state` está na
[ADR-001](../adr/ADR-001-ssm-vs-terraform-remote-state.md); o mapa completo dos parâmetros em
[componentes.md](../componentes.md#contrato-entre-repositórios-ssm-parameter-store).

Cada repositório de infraestrutura tem state próprio num bucket S3 privado, versionado e cifrado,
com **lock nativo** (`use_lockfile`, Terraform ≥ 1.11) — sem tabela DynamoDB para criar e destruir.

### Rede

VPC `10.0.0.0/16` em `us-east-1`, duas AZs, uma subnet pública e uma privada por AZ, todas `/20`
(o VPC CNI consome um IP da subnet por pod, e a *prefix delegation* reserva `/28` por nó).
Internet Gateway para as públicas; **um** NAT Gateway (não um por AZ) para as privadas; endpoint
S3 do tipo Gateway nas duas route tables, gratuito, que tira o `docker pull` do ECR do caminho pago
do NAT. As subnets carregam as tags `kubernetes.io/role/elb` e `kubernetes.io/role/internal-elb`:
sem elas o cloud controller do EKS não sabe onde criar um load balancer e o Service fica `<pending>`.

Nos privados moram os nós, o RDS e a Lambda. Um modo econômico (`enable_nat_gateway = false`) move
os nós para as públicas — alternar recria o node group, porque `subnet_ids` é *force-replacement*.
A VPC está escrita em recursos crus, sem módulo: [ADR-005](../adr/ADR-005-vpc-em-recursos-crus.md).

### Cluster EKS

Versão do Kubernetes deixada para a AWS escolher (`cluster_version = null` → sempre em *standard
support*; *extended support* custa 6× mais). Node group gerenciado com 2 × `t3.medium` AL2023
on-demand, disco de 20 GB, mín. 2 / máx. 4. `t3.small` daria 11 pods por nó e ~1,5 GiB — apertado
com os DaemonSets do New Relic. Addons `vpc-cni`, `kube-proxy`, `coredns` e `metrics-server`
(sem ele o HPA fica `<unknown>` e nunca escala). Sem log de control plane no CloudWatch (custa).

Endpoint da API público com `0.0.0.0/0` e privado ao mesmo tempo: [ADR-004](../adr/ADR-004-api-do-cluster-publica.md).

### Aplicação no cluster

Namespace `car-workshop`. Deployment com 2 réplicas de base, imagem do ECR com tag = SHA do commit
e `imagePullPolicy: Always`, container não-root (uid 1001, sem capabilities), `startupProbe` para o
boot lento (Quarkus + Flyway), `livenessProbe` em `/q/health/live` (não em `/ready`: readiness inclui
o banco, e um blip do RDS reiniciaria todos os pods). Service `LoadBalancer` sem annotation → Classic
LB em `:80 → :8080`. Exposição pública e a alternativa interna: [ADR-003](../adr/ADR-003-exposicao-da-app-lb-publico.md).

Três Secrets gerados do SSM a todo deploy — `car-workshop-db`, `car-workshop-app`, `car-workshop-jwt`
— e nenhum versionado. O par RSA do JWT é montado como **volume** (`/deployments/secrets`, modo
0440 com `fsGroup`), não como variável de ambiente: em env var apareceria no `kubectl describe pod`,
seria herdado por todo processo filho e entraria na captura de ambiente do agente New Relic.

### Autoescala (HPA)

`autoscaling/v2`, alvo de **CPU 60 %** (a métrica do demo — é ela que o gerador de carga move) e
**memória 80 %** (para cumprir a redação "CPU/memória" do enunciado), mín. 2, **máx. 4**.

Os dois números que exigiram medição:

- **`requests.memory = 896Mi` (= limit, QoS Guaranteed).** A fórmula do HPA é
  `desired = ceil(réplicas × utilização / alvo)`; descer de *n* para *n − 1* exige utilização abaixo
  de `alvo × (n − 1) / n` — **53 %** para voltar a 2, não 80 %. Com os 384Mi da Fase 2 o pod media
  84 % depois que o agente New Relic entrou na imagem, e o HPA subia sozinho por memória; com 512Mi
  caiu para 64–68 %, abaixo do alvo e ainda assim **travado em 4 réplicas** (2026-08-28), porque a JVM
  não devolve heap. Dimensionado pelo pior caso com o heap cheio (220Mi de não-heap + 25 % do limite
  via `MaxRAMPercentage`): 49,6 % com 896Mi; 53,6 % com 768Mi, que ainda travaria. Provado em
  2026-08-29 com 24 clientes concorrentes: CPU a 215 %, HPA de 2 → 6 em menos de 1 min sem `Pending`,
  memória em 39,8 % durante a carga, e ao cessar 6 → 5 → 4 → 3 → 2 em 8 min.
- **`maxReplicas = 4`, por bin-packing, não por escolha.** Histórico: 10 (Fase 2, prometia réplicas
  que ficariam `Pending`) → 6 (o que dois `t3.medium` comportam a 896Mi) → 5 (2026-09-01: os três
  Deployments do `nri-bundle` caem no mesmo nó por `podAffinity`) → 4 (2026-09-03: o
  `newrelic-prometheus-agent` pede 160Mi no nó leve). Um teto determinístico vale mais que um
  número que depende da alocação de pods do dia, porque a infraestrutura é recriada entre sessões e
  o agendador não reproduz a mesma distribuição. Validado com carga de escrita: 4 pods, 2 por nó,
  zero `Pending`.

### CI/CD

| Repositório | PR | Merge na `main` | Manual |
|---|---|---|---|
| 2, 3 | `terraform fmt`, `validate`, `plan` | `apply` | `workflow_dispatch` plan / apply / destroy |
| 1 | guardas (sessão, variáveis, contrato dos repos 2/3/4, sondagem HTTP do ELB), `npm ci`, `node --test`, `plan` | `apply` + smoke test do Gateway | idem |
| 4 | `mvn clean verify` (432 testes, JaCoCo ≥ 75 %) + Trivy | `verify` → `docker build` → push no ECR → Secrets do SSM → `kubectl apply` → `rollout status` → publica `/fase3/eks/lb-dns` → smoke test | `workflow_dispatch` para republicar a imagem sem commit vazio |

Um só ambiente, então merge na `main` **é** o deploy em produção: [ADR-002](../adr/ADR-002-ambiente-unico.md).
Os workflows dependentes do lab não são *status checks* obrigatórios — travariam todo merge fora da
sessão. Os quatro repositórios têm `main` protegida por ruleset (PR obrigatório, sem force push, sem
exclusão) e o último run da `main` verde, conferido em 2026-09-05.

### Custo

| Recurso | US$/h aprox. |
|---|---|
| EKS control plane | 0,10 |
| 2 × `t3.medium` | 0,083 |
| RDS `db.t3.micro` | 0,017 |
| 1 × NAT Gateway | 0,045 |
| **Total** | **~0,25 (~US$ 5,60/dia)** |

Um ciclo completo de subir, validar e derrubar custa ~US$ 0,40; uma sessão de 2h45 em 2026-09-04
custou ~US$ 0,69. A disciplina é `destroy` ao fim de cada sessão **com auditoria da conta item por
item** (script `destroy-all.ps1`: 15 categorias de recurso conferidas vazias em 2026-09-05), porque o
painel do lab atrasa e não serve como sinal de segurança.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Conta AWS própria, sem as restrições do lab | Custo real por fora do curso; e as restrições são o exercício — o desenho tem que funcionar onde a banca o avalia |
| Serverless Framework para a Lambda | Tenta criar IAM roles e bate na parede do lab; IaC único em Terraform nos três repositórios de infraestrutura |
| `cluster-autoscaler` / Karpenter | Exigem IRSA (OIDC provider + IAM role), impossível sem `iam:CreateRole`. Escalar nós é reaplicar `node_desired_size` |
| Um NAT por AZ | ~US$ 1,20/dia cada; perde-se HA de saída, aceitável num ambiente destruído entre sessões |
| Um único repositório com quatro diretórios | Um `apply` acidental do cluster ao mexer na Lambda; um único state com a senha do banco e o DNS do ELB juntos; um só pipeline para ciclos de vida distintos |
| `t3.small` nos nós | 11 pods por nó pelo limite de ENIs do VPC CNI e ~1,5 GiB — não cabe a aplicação, o bundle do New Relic e a folga do HPA |

## Consequências

- O ambiente existe só durante uma sessão: qualquer evidência (dashboard, autoescala, token) tem que
  ser gravada com o lab de pé. Nada fica "no ar" para a banca visitar depois.
- Os nomes das roles do EKS mudam a cada reset do lab e vivem em variáveis do GitHub e em
  `terraform.tfvars` (gitignored); errar uma delas só falha ~10 min depois, na criação do node group.
- `/fase3/eks/lb-dns` é publicado pelo pipeline da aplicação, não por Terraform: sobrevive ao destroy
  apontando para um ELB morto e faz o API Gateway nascer em 503 se o repositório 1 for aplicado antes
  do 4. Guardas no preflight e no CI do repositório 1 detectam isso.
- Sem cluster-autoscaler, o teto de réplicas é a capacidade dos dois nós — daí `maxReplicas = 4`.

## Evidências

| O quê | Onde |
|---|---|
| VPC, subnets, NAT, endpoint S3 | `fiap-fase3-infra-k8s/vpc.tf`, `variables.tf`, `locals.tf` |
| EKS, node group, addons, roles por `data source` | `fiap-fase3-infra-k8s/eks.tf` |
| ECR | `fiap-fase3-infra-k8s/ecr.tf` |
| Contrato SSM publicado | `fiap-fase3-infra-k8s/ssm.tf` |
| Deployment, Service, HPA, ConfigMap | `fiap-fase3-app/k8s/application/` |
| Pipelines | `.github/workflows/terraform.yml` nos repositórios 1, 2 e 3; `maven-ci.yml` e `cd.yml` no 4 |
| Apply do repositório 2 (34 recursos), 2 nós `Ready`, smoke tests de ECR/LB/NLB/SSM, destroy limpo | 2026-08-04 |
| Deploy pelo pipeline com imagem no SHA do merge, health 200 pelo ELB, JWT por volume, `env` sem PEM | 2026-08-29 e 2026-09-01 |
| Autoescala 2 → 6 → 2 sob carga; recalibragem 384 → 512 → 896Mi | 2026-08-28 e 2026-08-29 |
| `maxReplicas` 6 → 5 → 4 com 4 pods e zero `Pending` | 2026-09-01 e 2026-09-03 |
| Último run da `main` verde nos quatro repositórios; destroy com auditoria 15/15 | 2026-09-05 |
