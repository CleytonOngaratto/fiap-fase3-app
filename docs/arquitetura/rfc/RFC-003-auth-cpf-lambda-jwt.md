# RFC-003 — Autenticação do cliente por CPF: API Gateway + Lambda + JWT

| | |
|---|---|
| **Status** | Implementada e verificada pelo API Gateway provisionado pelo pipeline (2026-09-03) |
| **Data** | 2026-07-28 (decisão) · 2026-09-02 (implementação) |
| **Repositório** | `fiap-fase3-auth-serverless` (Lambda, API Gateway); contrato do token em `fiap-fase3-app` |

## Requisito

O cliente da oficina se identifica **apenas pelo CPF** e recebe um token para acompanhar suas ordens
de serviço. O enunciado pede que uma função serverless valide o CPF, confira o cliente no banco e
gere o JWT, e que as rotas da aplicação fiquem atrás de um API Gateway.

Duas leituras combinadas com os professores delimitam o escopo:

- Não existe coluna de status em `customers`. O requisito "cliente ativo" foi cumprido como
  **"cliente existe"**, sem migration nova.
- Um único ambiente: uma Lambda, um Gateway.

## Proposta

### Fluxo

```
CPF → API Gateway (POST /auth) → Lambda → valida dígitos → consulta RDS → assina JWT RS256 → token
                                                                                              │
      API Gateway (ANY /{proxy+}) ──────────────► LoadBalancer do EKS ◄──── Bearer ──────────┘
```

A sequência completa, com os códigos de resposta, está em [sequencia.md](../sequencia.md).

### Borda: API Gateway HTTP API

| Recurso | Configuração | Por quê |
|---|---|---|
| Tipo | **HTTP API** (`aws_apigatewayv2_api`), não REST API | Mais barato, sobe em segundos, e não exige a role de CloudWatch no nível da conta que o lab não deixaria criar |
| `POST /auth` | integração `AWS_PROXY`, payload 2.0, `aws_lambda_permission` para o Gateway invocar | Sem a permissão o Gateway leva 403 da Lambda e devolve 500 — erro que parece bug na função |
| `ANY /{proxy+}` | integração `HTTP_PROXY` para `http://<dns-do-lb>/{proxy}` | O DNS vem de `/fase3/eks/lb-dns`; `{proxy}` recebe o path capturado, então `/carworkshop/v1/tracking/1` chega inteiro ao ELB. Rota mais específica ganha: `POST /auth` não é engolido |
| Stage | `$default` com `auto_deploy` | Ambiente único: sem promoção entre stages, e a URL sai sem prefixo — o path que o cliente chama é o que a app recebe |
| Access log | log group próprio, 7 dias, com `integrationStatus` e `integrationErrorMessage` | Distingue "o ELB morreu" de "a app respondeu erro" — o diagnóstico do 503 quando `/fase3/eks/lb-dns` está obsoleto |
| `X-Trace-Id` | **não** carimbado na borda | `overwrite` apagaria o header do cliente e `append` produziria `id-cliente,id-gateway`; o id de fora atravessar intacto é a base da correlação log ↔ requisição. O id do Gateway fica no access log |

### Função: Lambda Node.js na VPC

| Configuração | Valor | Por quê |
|---|---|---|
| Runtime | `nodejs22.x`, 256 MB, 15 s | 128 MB fica apertado com o SDK do SSM carregado; 15 s cobre cold start com ENI na VPC + SSM + query |
| Execution role | `LabRole`, por `data source` | O lab não deixa criar IAM; é a única identidade com `ssm:GetParameters` e `kms:Decrypt` disponível |
| Rede | as 2 subnets privadas; SGs `[crachá do repo 3, SG próprio]` | O crachá dá 5432 ao RDS sem criar regra em SG alheio; o SG próprio dá egress 443 (SSM, via NAT) e 53 (resolver da VPC). SGs são aditivos |
| Empacotamento | `archive_file` + `npm ci --omit=dev` | O módulo `terraform-aws-modules/lambda/aws` empacota por script Python — um runtime a mais só para empacotar, que nem toda máquina terá. Preço: `npm ci` é passo obrigatório antes do `plan` — guardado no preflight e no CI, porque sem ele o apply fica verde e a função quebra com `Cannot find module` |
| Log group | explícito, 7 dias | Sem ele a Lambda cria um com retenção infinita que o destroy não remove |
| Concorrência reservada | não configurada | A AWS recusa reserva que deixe a conta com menos de 100 de concorrência não reservada, e o teto do lab é 10 |

### Segredos: lidos em runtime, nunca no state nem em env var

A função recebe por variável de ambiente só o que não é segredo (host, porta, banco, usuário,
issuer, TTL) e os **nomes** dos dois parâmetros sensíveis: `/fase3/jwt/private-key` e
`/fase3/rds/password`. Os valores são lidos no primeiro uso do container com `GetParameters` e
`WithDecryption`, e memoizados (`lambda/config.mjs`).

Os dois **não têm `data source`** no Terraform de propósito: um `data "aws_ssm_parameter"`
materializaria o valor decifrado em texto plano no state. E não vão em env var porque apareceriam no
console da função e na captura de ambiente de qualquer agente.

Medido em 2026-09-02/03: zero `AccessDenied` no log (a `LabRole` decifra o SecureString), cold start
de **890 ms**, invocação quente de **5,8 ms** (o cache dos segredos e o pool funcionam), 111 MB de 256.

### O contrato do token

A aplicação **já validava** JWT com SmallRye e **não foi alterada** para aceitar o token da Lambda. O
token tem que ser aceito pela validação como ela está:

| Configuração da app (`application.properties`) | Consequência na Lambda (`lambda/jwt.mjs`) |
|---|---|
| `mp.jwt.verify.publickey.location` → chave pública do volume | Assina com a **privada do mesmo par**, lida de `/fase3/jwt/private-key` |
| `mp.jwt.verify.issuer = https://oficina-api.com` | `iss` idêntico, byte a byte |
| `mp.jwt.verify.groups.path = groups` | Papéis na claim `groups`, como **array**: `["CUSTOMER"]` |
| `mp.jwt.verify.audiences` **não configurada** | **Não** emite `aud` — audiência que ninguém verifica só simula uma checagem |
| Token de admin expira em 3600 s (`JwtTokenAdapter`) | Mesmo TTL: `exp − iat = 3600` |

Resultado: **RS256** com `sub` = CPF, `iss`, `groups`, `cpf`, `iat`, `exp`. O formato espelha o
`JwtTokenAdapter` da app (`Jwt.claims().subject(...).issuer(...).groups(...).expiresIn(3600)`).

Do lado da aplicação, a única mudança foi de autorização: `/tracking/*` saiu de `@PermitAll` para
`@RolesAllowed({"CUSTOMER", "ADMIN"})`. As rotas administrativas seguem `ADMIN`.

O par RSA vive só no SSM (SecureString) e chega à app como volume de Secret e à Lambda em runtime —
o par que estava no classpath da Fase 2 foi removido e aposentado. Um par distinto e descartável
fica em `src/test/resources/` como fixture dos testes, que assinam offline.

### Validação do CPF: espelho da aplicação

`lambda/cpf.mjs` reproduz `CustomerDomainValidator.isValidCpf`: 11 dígitos, rejeita sequência
repetida, confere os dois dígitos verificadores. **Sem normalizar pontuação**, também por espelho:
a app exige `^\d{11}$` e a coluna guarda 11 dígitos crus; aceitar `529.982.247-25` aqui criaria dois
contratos diferentes no mesmo sistema, e um cliente existiria no banco sem conseguir token.

### Respostas

| Caso | Código | Por quê |
|---|---|---|
| Sem corpo, JSON inválido, `cpf` ausente ou vazio | 400 `invalid_request` | "Faltou o campo" é distinto de "documento errado" |
| Dígitos verificadores inválidos | 400 `invalid_cpf` | **Não 404**: responder "não encontrado" a um CPF impossível faria do endpoint um oráculo de quais CPFs estão cadastrados |
| CPF válido, cliente inexistente | 404 `customer_not_found` | **Não 401**: nada foi recusado por falta de credencial — o recurso é que não existe |
| Cliente existe | 200 `{access_token, token_type: Bearer, expires_in: 3600}` | |
| Erro interno | 500 `internal_error` | Causa só no CloudWatch; o CPF sai **mascarado** no log (`*********00`) porque é dado pessoal |

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Amazon Cognito | O fluxo é "só CPF, sem senha": exigiria user pool com custom auth challenge (três Lambdas de trigger) e uma segunda identidade paralela à que a app já tem. A app continuaria tendo que validar um token com issuer e chave diferentes |
| JWT authorizer no API Gateway | Exige issuer com endpoint JWKS público; a app não expõe JWKS (a chave é um PEM no SSM). E a validação já acontece na app pelo SmallRye — o authorizer duplicaria a checagem sem removê-la, porque o ELB continua alcançável |
| Lambda chamando um endpoint da app em vez do RDS | Criaria uma rota interna sem autenticação na app só para a Lambda, e uma dependência da app no caminho de emitir token. Decisão alinhada com o professor: acesso direto ao banco pela VPC |
| Serverless Framework | Tenta criar IAM roles e bate na parede do lab; IaC único em Terraform |
| REST API (API Gateway v1) | Mais caro, deploy explícito por stage, e o logging exige uma role de CloudWatch na conta que o lab não cria |
| Módulo `terraform-aws-modules/lambda/aws` | Empacota por Python; substituído por `archive_file` + `npm ci`, alternativa já prevista na decisão original |
| Segredos por `data "aws_ssm_parameter"` | Valor decifrado no state em texto plano |
| Carimbar `X-Trace-Id` no Gateway | Quebraria a correlação ponta a ponta (motivo acima) |

## Consequências

- **O Gateway não é o único caminho**: o ELB do Service é público e responde sem passar por ele. A
  validação do JWT está na app, então o ELB direto sem token dá 401 — mas a superfície é maior. Ver
  [ADR-003](../adr/ADR-003-exposicao-da-app-lb-publico.md).
- `/fase3/eks/lb-dns` é publicado pelo pipeline da aplicação e sobrevive ao destroy: se o repositório
  1 for aplicado com o parâmetro obsoleto, o `/{proxy+}` nasce em 503 **sem erro de apply**. Guardas
  no preflight e no CI (sondagem HTTP real do ELB) existem por isso.
- A Lambda na VPC depende do NAT do repositório 2 para alcançar o SSM. Com o modo econômico sem NAT,
  a função morre por timeout no cold start — que parece lentidão, não rota.
- `POST /auth` é enumerável por CPF válido (200 vs 404). Mitigação hoje: só CPFs com dígitos
  verificadores corretos chegam ao banco, e o log mascara. Rate limiting no Gateway é melhoria futura.
- Sem `aud`, qualquer serviço que confie na mesma chave pública aceitaria o token. Hoje só existe um.

## Evidências

| O quê | Onde |
|---|---|
| Lambda, SG próprio, env vars, empacotamento | `fiap-fase3-auth-serverless/lambda.tf` |
| Gateway, rotas, integrações, stage, access log | `fiap-fase3-auth-serverless/apigateway.tf` |
| Contrato lido do SSM e os dois segredos sem `data source` | `fiap-fase3-auth-serverless/data.tf`, `locals.tf` |
| Handler, validação de CPF, assinatura, leitura de segredos, acesso ao banco | `fiap-fase3-auth-serverless/lambda/{index,cpf,jwt,config,db}.mjs` |
| Configuração de verificação na app | `fiap-fase3-app/src/main/resources/application.properties` |
| Autorização de `/tracking/*` | `fiap-fase3-app/src/main/java/.../workorder/adapter/controller/WorkOrderTrackingController.java` |
| Validador de CPF espelhado | `fiap-fase3-app/src/main/java/.../customer/domain/validator/CustomerDomainValidator.java` |
| 22 testes offline (`node --test`); prova offline de que o SmallRye aceita o token assinado com a chave do SSM; reprodução local do `no pg_hba.conf entry` com Postgres `ssl=on` | 2026-09-02 |
| Apply de 14 recursos; 7 casos pelo Gateway (200 com JWT e claims corretas, 200 na rota protegida, 401 sem token, 404, 400 × 3); `X-Trace-Id` intacto; `plan` pós-apply sem diff | 2026-09-02 (local) e 2026-09-03 (pelo pipeline) |
| Cold start 890 ms, quente 5,8 ms, 111 MB, zero `AccessDenied`, CPF mascarado | 2026-09-02/03 |
