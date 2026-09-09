# RFC-002 — Banco de dados: RDS PostgreSQL 16

| | |
|---|---|
| **Status** | Implementada e verificada na AWS (apply em 2026-08-21; migrations conferidas no RDS em 2026-09-01) |
| **Data** | 2026-08-21 |
| **Repositório** | `fiap-fase3-infra-db` (instância, rede, senha, contrato); consumidores em `fiap-fase3-app` e `fiap-fase3-auth-serverless` |

## Motivação

Na Fase 2 o PostgreSQL era um pod no Minikube. Em nuvem isso vira dado que morre com o cluster,
backup e TLS por conta própria e um StatefulSet disputando os ~4 GiB de cada nó com a aplicação. O
enunciado pede banco gerenciado; o modelo relacional já estava justificado pelo domínio (ver
[banco-de-dados.md](../banco-de-dados.md)). Esta RFC decide **como** o RDS é provisionado, protegido
e consumido.

## Proposta

### Instância

| Parâmetro | Valor | Por quê |
|---|---|---|
| Engine | PostgreSQL, `engine_version = "16"` (prefixo) | Casa com o `postgres:16` do desenvolvimento; a AWS resolve o minor sem gerar diff a cada `plan` (`auto_minor_version_upgrade`) |
| Classe | `db.t3.micro` | O lab só libera micro/small/medium; single-AZ com carga de demo cabe folgado |
| Storage | gp2, 20 GB, `max_allocated_storage = 0` | gp3 é bloqueado no lab; 20 GB é o mínimo do gp2; autoscaling de storage sobe sozinho e **não desce** — custo permanente num crédito fixo |
| Criptografia em repouso | `storage_encrypted = true` com a chave gerenciada `aws/rds` | O lab não permite criar chave KMS própria |
| Multi-AZ | desligado | Dobra o custo e o ambiente é único ([ADR-002](../adr/ADR-002-ambiente-unico.md)); o subnet group ainda cobre duas AZs porque o RDS exige, e é onde ele recria a instância se a AZ cair |
| Acesso público | `publicly_accessible = false` | Só alcançável de dentro da VPC |
| Backup | 1 dia de retenção, `copy_tags_to_snapshot` | 1 é o mínimo que mantém o backup ligado; mais que isso é custo para um ambiente destruído entre sessões |
| Snapshot final | `skip_final_snapshot = true` | O snapshot sobreviveria ao destroy e seguiria cobrando |
| Proteção contra exclusão | desligada | O `terraform destroy` faz parte do fluxo de toda sessão |
| Extended support | `open-source-rds-extended-support-disabled` | Cobra por vCPU-hora — mais que a própria instância |
| Monitoring | `monitoring_interval = 0`, sem Performance Insights | Bloqueados no lab; enhanced monitoring exigiria criar uma IAM role |
| `apply_immediately` | true | A janela de manutenção cairia fora da sessão de ~4 h |

### TLS obrigatório

O parameter group default do PostgreSQL 16 no RDS traz `rds.force_ssl = 1`. Confirmado em
2026-08-21: a conexão sobe em TLS 1.3 e um cliente sem TLS é recusado com
`no pg_hba.conf entry ... no encryption` — erro que parece security group. A decisão foi **manter**:
TLS obrigatório é o comportamento certo, e quem se adapta é o cliente.

- **Aplicação:** `?sslmode=require` explícito na JDBC URL (`application.properties`,
  `DB_SSLMODE=require` no ConfigMap). O pgjdbc negociaria sozinho com o default `prefer`, mas o
  contrato fica visível em vez de depender de um default de driver.
- **Lambda:** o `pg` do Node nasce com `ssl: false`; a função usa `ssl: { rejectUnauthorized: false }`
  (`lambda/db.mjs`) — o equivalente exato do `require`: cifra sem verificar a CA. Verificar exigiria
  embutir o bundle de CAs da AWS no pacote.

### Acesso por pertencimento a security group, não por CIDR

Dois grupos:

- **`fiap-fase3-rds`**, anexado à instância. Ingress 5432 de exatamente duas origens: o **SG primário
  do cluster EKS** (`/fase3/eks/node-sg-id` — node group gerenciado sem launch template não tem SG
  próprio, os nós herdam o do cluster) e o grupo abaixo. **Sem egress**: o RDS nunca inicia conexão.
- **`fiap-fase3-rds-client`**, o **crachá**. Nasce vazio e não protege nada: quem o anexa passa a
  alcançar o banco. Publicado em `/fase3/rds/client-sg-id`.

O crachá existe por um motivo concreto: a Lambda do repositório 1 precisa do banco, mas o security
group dela não existe quando o repositório 3 é aplicado. As alternativas eram liberar o CIDR inteiro
da VPC (qualquer coisa na rede alcançaria o banco) ou fazer o repositório 1 criar uma regra dentro
de um SG que não é dele. Com o crachá, o repositório 1 lê um id do SSM e anexa — o mesmo padrão de
contrato de todo o resto. Security groups são aditivos e a Lambda aceita até cinco, então ela anexa
o crachá **e** um SG próprio com o egress 443 para o SSM.

Conferido em 2026-08-21: ingress do SG do banco só do crachá e do SG do cluster, egress vazio; `psql`
de um pod do EKS conecta. Em 2026-09-02: as ENIs da Lambda carregam `[SG próprio, crachá]`.

### Senha

`random_password` de 24 caracteres (alfabeto especial restrito a `*()-_=+[]{}` — o RDS recusa `/`,
`@`, `"` e espaço, e o resto ainda precisa atravessar URI e shell) → `/fase3/rds/password` como
**SecureString**. A senha nunca existe no git nem em disco.

Ela **fica no state** do Terraform — e as alternativas registradas também ficavam: um
`data "aws_ssm_parameter"` materializa o valor decifrado como atributo. Só `password_wo` (write-only)
evitaria, ao custo de um modo de falha silencioso: o valor ephemeral é regerado a cada execução e só
o `*_wo_version` decide se é empurrado, então um apply interrompido entre o SSM e o RDS deixaria os
dois divergentes sem erro. Mitigação adotada: bucket de state privado, versionado e cifrado, e senha
**regerada a cada sessão** com o `destroy` + `apply`.

Consequência para os consumidores: a senha muda a cada recriação do banco, então o Secret do
Kubernetes é **regerado do SSM a todo deploy**, e a Lambda a lê em runtime — nunca no apply.

### Contrato publicado

| Parâmetro | Valor | Observação |
|---|---|---|
| `/fase3/rds/endpoint` | `.address` da instância | **host puro, sem porta** — `.endpoint` vem como `host:5432` e produziria `host:5432:5432` ao lado do parâmetro de porta |
| `/fase3/rds/port` | 5432 | |
| `/fase3/rds/db-name` | `oficina_db` | Tem que casar com a app (Flyway) |
| `/fase3/rds/username` | master user | Dono do banco, então o Flyway escreve no schema `public` |
| `/fase3/rds/password` | SecureString | |
| `/fase3/rds/client-sg-id` | id do crachá | |

O repositório lê do 2: `/fase3/vpc/id`, `/fase3/vpc/private-subnets`, `/fase3/eks/node-sg-id`.
Nenhum id de VPC, subnet ou SG é hardcodado.

### Schema

O Flyway roda **na aplicação** (`migrate-at-start`), não na infraestrutura. O repositório 3 entrega
uma instância com um banco vazio; a primeira réplica que sobe aplica V1 → V4, as demais validam. Em
2026-08-27 e 2026-09-01: `Successfully validated 4 migrations`, `schema "public": 4.0.0`, e as quatro
linhas de `flyway_schema_history` com `success = t` lidas no próprio RDS.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| PostgreSQL no cluster (StatefulSet + PVC) | Morre com o cluster que é destruído toda sessão; backup, TLS e failover por conta própria; disputa memória com a aplicação nos dois nós |
| Aurora Serverless v2 | Mínimo de 0,5 ACU cobrado continuamente e classe fora do que o lab libera; nenhuma feature usada pelo domínio |
| DynamoDB ou outro NoSQL | O domínio é relacional (N:M entre OS e serviços/peças, estoque transacional); a consistência eventual não serve para aprovar orçamento e baixar estoque |
| Multi-AZ | Dobra o custo para um ambiente único que é destruído entre sessões |
| Regra de ingress por CIDR da VPC | Qualquer recurso na rede alcançaria o banco; o crachá dá regra mínima sem que um repositório mexa no SG do outro |
| Repositório 1 criando regra no SG do RDS | Recurso de um repositório dentro do state de outro — quebra o isolamento que os quatro repositórios existem para dar |
| `password_wo` | Modo de falha silencioso descrito acima |
| Senha fixa em variável | Existiria no git ou num Secret manual; a geração aleatória por sessão é mais simples e mais segura |

## Consequências

- Recriar o banco apaga os usuários da aplicação (admins criados por `POST /auth/signup`) e as OS
  de demonstração. Todo início de sessão recadastra o que a gravação precisar.
- `/fase3/rds/endpoint` muda a cada recriação. Como nada é hardcodado, basta reaplicar os
  consumidores na ordem 4 → 1.
- Sem Multi-AZ, a perda da AZ da instância significa indisponibilidade até o RDS recriá-la na outra
  AZ do subnet group.
- A senha no state é um risco aceito e documentado, não escondido.

## Evidências

| O quê | Onde |
|---|---|
| Instância, subnet group, senha | `fiap-fase3-infra-db/rds.tf`, `variables.tf` |
| Os dois security groups e as regras | `fiap-fase3-infra-db/security.tf` |
| Contrato lido e publicado | `fiap-fase3-infra-db/data.tf`, `ssm.tf`, `locals.tf` |
| Consumo pela app (JDBC com `sslmode`) | `fiap-fase3-app/src/main/resources/application.properties`, `k8s/application/configmap.yaml` |
| Consumo pela Lambda (`pg` com TLS, crachá anexado) | `fiap-fase3-auth-serverless/lambda/db.mjs`, `lambda.tf` |
| Apply de 14 recursos, instância `available` em 6m33s, `plan` pós-apply sem diff, `psql` de pod | 2026-08-21 |
| `rds.force_ssl = 1` confirmado com cliente sem TLS recusado | 2026-08-21 |
| Migrations V1 → V4 conferidas no RDS | 2026-08-28 e 2026-09-01 |
| Ids de sequence sem sobreposição entre réplicas (`allocationSize = 1`), 20/20 escritas concorrentes | 2026-09-03 |
