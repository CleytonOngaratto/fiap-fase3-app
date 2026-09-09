# ADR-003 — Exposição da aplicação: LoadBalancer público + HTTP proxy no API Gateway

| | |
|---|---|
| **Status** | Aceita e implementada; alternativa interna provada viável e registrada como evolução |
| **Data** | 2026-07-28 (decisão) · 2026-08-04 (as duas opções testadas) · 2026-09-05 (evidência de tráfego não solicitado) |
| **Afeta** | `fiap-fase3-app` (Service), `fiap-fase3-auth-serverless` (integração do `/{proxy+}`), `fiap-fase3-infra-k8s` (tags das subnets) |

## Contexto

O API Gateway HTTP API precisa alcançar a aplicação no EKS. Há dois caminhos:

1. **Service `type: LoadBalancer` público** (Classic LB nas subnets públicas) e integração
   `HTTP_PROXY` do Gateway para o DNS dele.
2. **NLB interno** (subnets privadas, annotation `internal`) e integração via **VPC Link v2** do
   Gateway, sem nada exposto à internet além do próprio Gateway.

Os dois foram testados de fato em 2026-08-04, com o cluster de pé: o LB público respondeu HTTP 200
pelo DNS, e o NLB interno subiu com targets `healthy` (a permissão de ELB do role do cluster cobre
os dois). Ou seja, a escolha não foi por impossibilidade.

## Decisão

**LB público + HTTP proxy.** O Service não tem annotation (o cloud controller cria um Classic LB),
escuta em `:80 → :8080`, e o pipeline publica o hostname em `/fase3/eks/lb-dns`; o Gateway lê o
parâmetro e integra `ANY /{proxy+}` com `http://<dns-do-lb>/{proxy}`.

Motivos: menos recursos (sem NLB, sem VPC Link, sem security group para o link), o caminho já
provado no smoke test, um `curl` direto no ELB como ferramenta de diagnóstico quando o Gateway
devolve 503, e o `/fase3/eks/lb-dns` como único ponto de acoplamento entre os repositórios 4 e 1.

## O argumento contra, dito corretamente

Não é "o LB público fura a autenticação". **Não fura**: quem valida o JWT é a aplicação (SmallRye),
não o Gateway. Um `curl` direto no ELB sem token recebe **401 do Quarkus** — verificado em
2026-08-28, 2026-08-29 e 2026-09-01. Com token válido, o ELB direto e o Gateway devolvem o mesmo.

O argumento é **superfície de exposição**: existem duas portas de entrada, e uma delas não passa pelo
Gateway (sem access log dele, sem possibilidade de throttling ou WAF na borda).

Evidência concreta, não teórica: em 2026-09-04/05 o painel "Requisições recusadas por regra de
negócio (4xx)" do dashboard capturou tráfego que **não veio do gerador de carga** — `POST
/cgi-bin/%%32%65%%32%65/.../bin/sh` (path traversal ofuscado: `%%32%65` decodifica para `.`), `GET
/index.php` e fingerprinting em `/bad-request`. É bot varrendo o ELB público, que está em
`0.0.0.0/0`. A aplicação respondeu 400/404 corretamente e **não há vulnerabilidade** — mas é tráfego
que um NLB interno nunca receberia, e que um scanner mais insistente transformaria em ruído nos
painéis e no alerta de latência.

## Consequências

- Duas entradas: o Gateway (oficial, com `POST /auth`, access log e o DNS que a banca usa) e o ELB
  (alcançável por quem descobrir o hostname). A autorização é a mesma nas duas.
- O alerta "Falha no processamento de OS" olha só 5xx nas rotas de OS, então varredura de bot (4xx em
  paths inexistentes) não o dispara. O de latência olha a média de requisições com sucesso; um
  scanner de 404 não entra na conta.
- `/fase3/eks/lb-dns` muda a cada recriação do cluster e sobrevive ao destroy — a consequência está
  detalhada na [ADR-001](ADR-001-ssm-vs-terraform-remote-state.md).
- Sem TLS entre Gateway e ELB (o Gateway termina HTTPS; o trecho até o ELB é HTTP dentro da AWS). O
  lab não garante ACM para colocar certificado no ELB.

## Evolução registrada

**NLB interno + VPC Link v2** está provado e documentado como o próximo passo se a superfície virar
requisito: annotation `service.beta.kubernetes.io/aws-load-balancer-internal: "true"` (as subnets
privadas já têm a tag `kubernetes.io/role/internal-elb`), `aws_apigatewayv2_vpc_link` nas subnets
privadas com um SG próprio, e a integração do `/{proxy+}` passa a apontar para o listener do NLB via
`connection_type = "VPC_LINK"`. Custo: um NLB (~US$ 0,02/h) e o VPC Link; e o `curl` de dentro do
cluster deixa de servir como teste (NLB não faz hairpinning — a prova passa a ser
`describe-target-health`).

A outra mitigação — restringir o SG do ELB aos IPs do Gateway — não existe para HTTP API: não há
lista fixa de IPs de saída.

## Alternativas descartadas

| Alternativa | Por que não agora |
|---|---|
| NLB interno + VPC Link v2 | Mais três recursos e um SG para um ganho de superfície que o ambiente destruído entre sessões não exige; fica como evolução, já provada |
| ALB por Ingress (AWS Load Balancer Controller) | O controller precisa de IAM role via IRSA — impossível sem `iam:CreateRole` |
| NodePort + Gateway apontando para os nós | IPs dos nós mudam a cada node group; sem health check de aplicação; expõe a porta em todos os nós |
| Ingress NGINX + Service LoadBalancer | Um salto a mais para uma única aplicação; o LB continuaria público |

## Evidências

| O quê | Onde |
|---|---|
| Service sem annotation, `:80 → :8080`, com o aviso de apagar antes do destroy | `fiap-fase3-app/k8s/application/service.yaml` |
| Integração `HTTP_PROXY` para o DNS lido do SSM | `fiap-fase3-auth-serverless/apigateway.tf`, `locals.tf` |
| Tags `kubernetes.io/role/elb` e `internal-elb` nas subnets (as duas opções são possíveis) | `fiap-fase3-infra-k8s/vpc.tf` |
| Autorização na aplicação, não na borda | `application.properties` (`mp.jwt.verify.*`), `WorkOrderTrackingController.java` |
| Smoke tests: LB público HTTP 200 e NLB interno com targets `healthy` | 2026-08-04 |
| ELB direto sem token → 401; com token → 200 | 2026-08-28, 2026-08-29, 2026-09-01 |
| Tráfego de varredura capturado na tabela de 4xx do dashboard (`k8s/newrelic/dashboard.json`) | 2026-09-04/05 |
