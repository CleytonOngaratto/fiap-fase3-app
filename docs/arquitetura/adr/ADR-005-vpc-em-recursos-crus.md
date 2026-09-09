# ADR-005 — VPC em recursos crus, sem o módulo `terraform-aws-modules/vpc`

| | |
|---|---|
| **Status** | Aceita e implementada |
| **Data** | 2026-08-04 |
| **Afeta** | `fiap-fase3-infra-k8s` (`vpc.tf`, `variables.tf`, `locals.tf`) |

## Contexto

A forma usual de criar uma VPC para EKS em Terraform é o módulo da comunidade
`terraform-aws-modules/vpc/aws`: um bloco de ~20 linhas que entrega VPC, subnets, IGW, NAT, route
tables e tags. A alternativa é escrever cada recurso.

Este repositório é material de avaliação: as decisões de rede precisam estar **visíveis** para quem
lê o código e para as RFCs que o explicam.

## Decisão

Escrever a VPC em recursos crus: `aws_vpc`, `aws_subnet` × 4, `aws_internet_gateway`, `aws_eip`,
`aws_nat_gateway`, `aws_route_table` × 3, `aws_route`, `aws_route_table_association` × 4,
`aws_vpc_endpoint`. Cerca de 120 linhas em `vpc.tf`, cada decisão com o comentário do porquê ao lado.

## Motivos

- **Cada escolha fica explícita e discutível.** "Por que um NAT e não um por AZ?" está no recurso
  (`count = var.enable_nat_gateway ? 1 : 0`, com o custo no comentário). "Por que essa tag?" está na
  subnet (`kubernetes.io/role/elb` — sem ela o Service fica `<pending>`). "Por que o endpoint S3 nas
  duas route tables?" está no recurso (cobre o modo sem NAT). No módulo, tudo isso vira uma variável
  cujo efeito exige ler o código do módulo.
- **Dependência que o módulo esconde.** O NAT pode nascer antes do IGW estar anexado e ficar sem
  rota; o `depends_on = [aws_internet_gateway.main]` explícito resolve e documenta.
- **Modo econômico próprio.** `enable_nat_gateway = false` move os nós para as subnets públicas
  (`local.node_subnet_ids`), porque sem NAT as privadas não têm saída e o nó nunca completa o
  registro. É uma lógica deste projeto, não do módulo.
- **Uma dependência a menos.** O módulo traz constraint de versão de provider e um changelog
  próprio; num projeto de três repositórios de Terraform pequenos, é superfície sem retorno.
- **Tamanho.** A VPC deste projeto é pequena (2 AZs, 4 subnets); os recursos crus não passam de uma
  tela. O argumento de economia de linhas do módulo pesa em VPCs maiores.

## Consequências

- Mais linhas para manter do que o bloco do módulo — e são linhas que o `plan` mostra recurso a
  recurso, o que ajudou a diagnosticar o node group durante o Bloco 2.
- Sem as conveniências prontas do módulo: flow logs, IPv6, subnets de banco e de ElastiCache
  separadas, endpoints de interface. Nenhuma foi necessária.
- `subnet_ids` do node group é *force-replacement*: alternar `enable_nat_gateway` recria o node group
  (~10 min). O módulo não mudaria isso, mas a lógica está aqui e o comentário avisa.
- Quem conhece o módulo precisa ler o arquivo em vez de reconhecer o bloco — aceito, porque ler o
  arquivo é o objetivo.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| `terraform-aws-modules/vpc/aws` | Esconde as decisões atrás de variáveis; exigiria explicar na RFC o que o módulo faz por baixo; mais um constraint de versão |
| Módulo local próprio (`modules/vpc`) | Indireção sem reuso: só um consumidor; e a leitura passaria a exigir dois arquivos |
| VPC default da conta | Sem subnets privadas, sem tags do EKS, compartilhada com o que mais existir no lab, e não é destruível pelo `destroy` |

## Evidências

| O quê | Onde |
|---|---|
| Os recursos, com o porquê de cada um em comentário | `fiap-fase3-infra-k8s/vpc.tf` |
| Variáveis de CIDR, NAT opcional e o aviso de force-replacement | `fiap-fase3-infra-k8s/variables.tf` |
| Lógica do modo econômico | `fiap-fase3-infra-k8s/locals.tf` |
| Apply de 34 recursos, 2 nós `Ready`, LB público e NLB interno criados nas subnets certas pelas tags | 2026-08-04 |
| Tabela de decisões do repositório | `fiap-fase3-infra-k8s/README.md`, seção Decisões |
