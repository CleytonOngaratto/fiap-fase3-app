# Documentação arquitetural — Fase 3 (operação em nuvem)

Evolução da Car Workshop API (Fase 2) para a AWS: API Gateway na borda, autenticação do cliente por
CPF numa Lambda, aplicação no EKS, banco no RDS e observabilidade no New Relic — tudo provisionado
por Terraform em quatro repositórios.

Os diagramas C4 da Fase 2 ([`../c4model/`](../c4model)) continuam válidos como **estado anterior**:
a aplicação e o banco são os mesmos, agora hospedados em EKS e RDS. Os documentos abaixo descrevem o
que mudou ao redor deles.

## Documentos

| Documento | O que responde |
|---|---|
| [componentes.md](componentes.md) | Diagrama de componentes: nuvem, APIs, banco, monitoramento, CI/CD e o contrato SSM entre os repositórios |
| [sequencia.md](sequencia.md) | Diagramas de sequência: autenticação por CPF (Gateway + Lambda + JWT) e abertura de ordem de serviço |
| [banco-de-dados.md](banco-de-dados.md) | Justificativa do banco, diagrama ER e o desacoplamento por `Long id` + snapshot das migrations V3/V4 |
| [rfc/RFC-001-nuvem-aws.md](rfc/RFC-001-nuvem-aws.md) | Nuvem AWS: como o Learner Lab molda o desenho, os quatro repositórios, rede, EKS, autoescala, CI/CD e custo |
| [rfc/RFC-002-banco-rds-postgres.md](rfc/RFC-002-banco-rds-postgres.md) | RDS PostgreSQL 16: dimensionamento, TLS, acesso por security group, senha e contrato |
| [rfc/RFC-003-auth-cpf-lambda-jwt.md](rfc/RFC-003-auth-cpf-lambda-jwt.md) | Autenticação: API Gateway + Lambda + JWT RS256 aceito pela aplicação sem alteração |
| [adr/ADR-001-ssm-vs-terraform-remote-state.md](adr/ADR-001-ssm-vs-terraform-remote-state.md) | Contrato entre repositórios por SSM Parameter Store, não por `terraform_remote_state` |
| [adr/ADR-002-ambiente-unico.md](adr/ADR-002-ambiente-unico.md) | Um único ambiente (produção) |
| [adr/ADR-003-exposicao-da-app-lb-publico.md](adr/ADR-003-exposicao-da-app-lb-publico.md) | Aplicação exposta por LoadBalancer público + HTTP proxy, não por NLB interno + VPC Link |
| [adr/ADR-004-api-do-cluster-publica.md](adr/ADR-004-api-do-cluster-publica.md) | Endpoint da API do EKS aberto a `0.0.0.0/0` para o GitHub Actions |
| [adr/ADR-005-vpc-em-recursos-crus.md](adr/ADR-005-vpc-em-recursos-crus.md) | VPC escrita em recursos crus em vez do módulo da comunidade |

## Onde está cada tema pedido no Tech Challenge

| Tema do enunciado | Documento |
|---|---|
| Diagrama de componentes (nuvem, APIs, banco, monitoramento) | [componentes.md](componentes.md) |
| Diagrama de sequência (autenticação por CPF, abertura de OS) | [sequencia.md](sequencia.md) |
| RFC — nuvem | [RFC-001](rfc/RFC-001-nuvem-aws.md) |
| RFC — banco de dados | [RFC-002](rfc/RFC-002-banco-rds-postgres.md) |
| RFC — estratégia de autenticação | [RFC-003](rfc/RFC-003-auth-cpf-lambda-jwt.md) |
| ADR — padrão de comunicação entre componentes | [ADR-001](adr/ADR-001-ssm-vs-terraform-remote-state.md) (contrato SSM), [RFC-001 §Comunicação](rfc/RFC-001-nuvem-aws.md), [RFC-003](rfc/RFC-003-auth-cpf-lambda-jwt.md) (Gateway → Lambda, Gateway → ELB) |
| ADR — uso de HPA | [RFC-001 §Autoescala](rfc/RFC-001-nuvem-aws.md), [componentes.md](componentes.md) |
| ADR — divisão em quatro repositórios | [RFC-001 §Quatro repositórios](rfc/RFC-001-nuvem-aws.md), [ADR-001](adr/ADR-001-ssm-vs-terraform-remote-state.md) |
| ADR — autenticação serverless | [RFC-003](rfc/RFC-003-auth-cpf-lambda-jwt.md), [ADR-003](adr/ADR-003-exposicao-da-app-lb-publico.md) |
| Justificativa do banco, ER e relacionamentos | [banco-de-dados.md](banco-de-dados.md), [RFC-002](rfc/RFC-002-banco-rds-postgres.md) |

## Ordem de leitura sugerida

1. [componentes.md](componentes.md) — o mapa.
2. [sequencia.md](sequencia.md) — os dois fluxos do enunciado.
3. [RFC-001](rfc/RFC-001-nuvem-aws.md) → [RFC-002](rfc/RFC-002-banco-rds-postgres.md) → [RFC-003](rfc/RFC-003-auth-cpf-lambda-jwt.md).
4. As cinco ADRs, na ordem numérica.
5. [banco-de-dados.md](banco-de-dados.md).

## Repositórios

| # | Repositório | Conteúdo | Publica no SSM |
|---|---|---|---|
| 2 | [fiap-fase3-infra-k8s](https://github.com/CleytonOngaratto/fiap-fase3-infra-k8s) | VPC, EKS, ECR (Terraform) | `/fase3/vpc/*`, `/fase3/eks/*`, `/fase3/ecr/*` |
| 3 | [fiap-fase3-infra-db](https://github.com/CleytonOngaratto/fiap-fase3-infra-db) | RDS PostgreSQL e security groups (Terraform) | `/fase3/rds/*` |
| 4 | [fiap-fase3-app](https://github.com/CleytonOngaratto/fiap-fase3-app) | Aplicação Quarkus, manifestos EKS, New Relic, CI/CD (este repositório) | `/fase3/eks/lb-dns` |
| 1 | [fiap-fase3-auth-serverless](https://github.com/CleytonOngaratto/fiap-fase3-auth-serverless) | Lambda de autenticação por CPF e API Gateway (Terraform) | — |

Ordem de deploy: **2 → 3 → 4 → 1**. Ordem de destroy: **1 → 4 → 3 → 2**.

## Convenções

- Diagramas em **Mermaid**, renderizados pelo próprio GitHub — nenhuma ferramenta necessária.
- Cada RFC e ADR termina com uma seção **Evidências**: arquivo do repositório que materializa a
  decisão e data em que ela foi verificada na AWS.
- Nenhum documento contém segredo, endpoint, hostname de load balancer ou identificador de conta:
  esses valores vivem no SSM Parameter Store e mudam a cada sessão do laboratório.
