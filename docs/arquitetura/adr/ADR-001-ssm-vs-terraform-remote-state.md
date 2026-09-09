# ADR-001 — Contrato entre repositórios por SSM Parameter Store, não por `terraform_remote_state`

| | |
|---|---|
| **Status** | Aceita e implementada |
| **Data** | 2026-07-28 (decisão) · 2026-08-21 (senha do RDS incluída) · 2026-09-05 (revisão) |
| **Afeta** | Os quatro repositórios |

## Contexto

Quatro repositórios com state próprio precisam trocar identificadores: o RDS precisa da VPC e das
subnets; a aplicação precisa do ECR, do cluster e das credenciais do banco; o API Gateway precisa do
DNS do LoadBalancer. Hardcodar está fora de questão — os valores mudam a cada recriação da
infraestrutura, várias vezes por semana.

As duas formas idiomáticas de um Terraform ler o output de outro são `data "terraform_remote_state"`
(abrir o state do outro repositório no S3) ou um serviço de configuração intermediário.

## Decisão

Cada repositório **publica** o que cria em `aws_ssm_parameter` sob `/fase3/*` e **lê** o que precisa
por `data "aws_ssm_parameter"`. Segredos (senha do RDS, par RSA do JWT, chaves do New Relic,
*pepper* de senha) são `SecureString` com a chave gerenciada `aws/ssm`. Parâmetros que nenhum
Terraform cria (as chaves) são publicados uma vez pelo dono e sobrevivem ao `destroy`.

O mapa completo está em [componentes.md](../componentes.md#contrato-entre-repositórios-ssm-parameter-store).

## Motivos

1. **O state carrega tudo em texto plano.** A senha do RDS (`random_password`) e qualquer atributo
   sensível ficam no `terraform.tfstate`. Ler o state de outro repositório exigiria dar
   `s3:GetObject` no state **inteiro** a cada pipeline consumidor — quatro pipelines lendo a senha
   do banco para descobrir o id de uma VPC.
2. **Exposição mínima.** O SSM expõe só o que foi publicado, com IAM por path (`/fase3/rds/*`) se a
   conta permitir, e o valor cifrado onde importa.
3. **Desacoplamento de versão.** `terraform_remote_state` amarra os repositórios à versão do formato
   de state e ao nome dos outputs; um `terraform state mv` no repositório 2 quebraria o 3. Com SSM,
   o contrato é o nome do parâmetro.
4. **Leitores que não são Terraform.** O pipeline da aplicação (`kubectl`), o `deploy.ps1`, a Lambda
   em runtime e os scripts do New Relic leem o mesmo contrato com o AWS CLI ou SDK — o state não
   serviria a nenhum deles.
5. **Custo zero.** Parâmetros standard do SSM não cobram; Secrets Manager cobraria por segredo e por
   chamada.

## Consequências

Boas:

- Nenhum ARN, endpoint ou DNS aparece em código de outro repositório (conferido: o repositório 1
  lê 11 parâmetros e não hardcoda nada).
- A senha do RDS muda a cada sessão e ninguém precisa saber: quem consome relê o SSM.
- O `preflight.ps1` de cada repositório valida o contrato **antes** do `apply`, com mensagem que
  aponta o repositório que falta aplicar.

Ruins, e como foram tratadas:

- **`/fase3/eks/lb-dns` é publicado pelo `cd.yml` da aplicação, não por Terraform.** Nenhum `destroy`
  o apaga; ele sobrevive entre sessões apontando para um ELB morto, o `apply` do repositório 1 passa,
  e o `/{proxy+}` nasce em **503 sem erro de apply**. Tratamento: o preflight do repositório 1 e o
  step `Check EKS LoadBalancer is alive` do CI fazem uma sondagem HTTP real do ELB antes do `plan`; o
  `workflow_dispatch` do repositório 4 republica o parâmetro sem commit.
- **O provider marca todo `aws_ssm_parameter` como sensitive**, inclusive String comum. Sem
  `nonsensitive()` o `plan` esconde `integration_uri` e `DB_HOST` atrás de `(sensitive value)` —
  justamente os campos que denunciam um DNS obsoleto. Tratamento: `nonsensitive()` nos locals do
  repositório 1.
- **Um `data "aws_ssm_parameter"` de SecureString materializa o valor decifrado no state.** Por isso
  a Lambda **não** tem data source para a chave privada nem para a senha: recebe só os nomes por env
  var e lê os valores em runtime.
- **O tipo de um parâmetro não pode mudar depois de criado** (String ↔ StringList). Errar o tipo ao
  republicar à mão quebra o `apply` do repositório dono na sessão seguinte.
- **A senha do RDS fica no state do repositório 3 de qualquer jeito** — `random_password` é um
  recurso. As alternativas também a deixariam; a mitigação é bucket privado, versionado, cifrado e
  senha regerada por sessão (ver [RFC-002](../rfc/RFC-002-banco-rds-postgres.md)).
- `/fase3/vpc/cidr` ficou órfão por um mês (publicado pelo 2, consumido por ninguém) até o
  repositório 1 usá-lo nas regras de DNS do SG da Lambda. Um parâmetro sem consumidor não custa nada,
  mas o contrato precisa de um mapa para ninguém procurar o consumidor que não existe.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| `data "terraform_remote_state"` | Leitura do state inteiro (com a senha) por todo consumidor; acoplamento de versão e de nome de output; inútil para `kubectl`, Lambda e scripts |
| Hardcodar valores entre repositórios | Mudam a cada recriação; commit para cada sessão do lab |
| AWS Secrets Manager para tudo | Custo por segredo e por chamada; o SSM SecureString resolve com a mesma chave KMS gerenciada |
| Outputs de Terraform Cloud / workspaces compartilhados | Um repositório de fora do lab e mais uma credencial; e continuaria não servindo aos leitores que não são Terraform |
| Um único state para os quatro repositórios | Volta o blast radius: um `apply` na Lambda toca o cluster; e a senha, o DNS do ELB e a chave RSA ficam no mesmo arquivo |

## Evidências

| O quê | Onde |
|---|---|
| Publicação | `fiap-fase3-infra-k8s/ssm.tf`, `fiap-fase3-infra-db/ssm.tf`, step `Publish /fase3/eks/lb-dns` em `fiap-fase3-app/.github/workflows/cd.yml` |
| Leitura | `fiap-fase3-infra-db/data.tf`, `fiap-fase3-auth-serverless/data.tf` (com o comentário sobre os dois segredos sem data source), `locals.tf` com `nonsensitive()` |
| Leitura em runtime pela Lambda | `fiap-fase3-auth-serverless/lambda/config.mjs` |
| Secrets do Kubernetes gerados do SSM | `fiap-fase3-app/scripts/deploy.ps1`, step `Render Secrets from SSM` do `cd.yml` |
| Guarda contra o `lb-dns` obsoleto | `fiap-fase3-auth-serverless/scripts/preflight.ps1`, step `Check EKS LoadBalancer is alive` do `terraform.yml` |
| `ssm:PutParameter`/`GetParameter` viáveis no lab; os 3 repositórios de infraestrutura aplicados só com o contrato | 2026-08-04, 2026-08-21, 2026-09-02 |
| Parâmetros de bootstrap sobrevivem ao destroy (7 em `/fase3/*` após auditoria da conta vazia) | 2026-09-05 |
