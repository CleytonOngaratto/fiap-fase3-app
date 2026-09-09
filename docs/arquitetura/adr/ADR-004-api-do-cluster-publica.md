# ADR-004 — Endpoint da API do cluster EKS público (`0.0.0.0/0`)

| | |
|---|---|
| **Status** | Aceita e implementada |
| **Data** | 2026-08-04 |
| **Afeta** | `fiap-fase3-infra-k8s` (`eks.tf`, `variables.tf`); o `cd.yml` de `fiap-fase3-app` depende disto |

## Contexto

O pipeline da aplicação roda `aws eks update-kubeconfig` e `kubectl apply` a partir de um runner
hospedado do GitHub Actions, cujo IP de saída é **dinâmico** e sai de uma lista extensa e mutável. O
endpoint da API do EKS pode ser privado (só de dentro da VPC), público com allowlist de CIDRs, ou
público aberto.

O Learner Lab bloqueia `iam:CreateRole`, o que tira de cena as soluções que dependem de identidade
nova: OIDC do GitHub para assumir role, IRSA, instance profile para um runner na VPC.

## Decisão

`endpoint_public_access = true` com `public_access_cidrs = ["0.0.0.0/0"]`, **e** `endpoint_private_access
= true` para que os nós e os pods falem com o control plane pela rede privada.

O acesso continua **autenticado e autorizado**: o endpoint exige token IAM (`aws eks
get-token`) e RBAC do Kubernetes. O modo `API_AND_CONFIG_MAP` mantém o `aws-auth` como caminho de
join dos nós, e `bootstrap_cluster_creator_admin_permissions = true` dá admin a quem roda o `apply` —
é *create-only*: mudar depois recria o cluster.

Melhor nós levantarmos este ponto que o avaliador.

## Consequências

- O endpoint da API é **enumerável** na internet: responde ao TLS handshake e ao `401` de quem não
  tem token. Não é acesso, mas é presença.
- A segurança do cluster passa a depender inteiramente das credenciais IAM do lab (temporárias, ~4 h)
  e do RBAC. Um vazamento dos três GitHub Secrets daria `kubectl` admin até a sessão expirar.
- Sem log de control plane no CloudWatch (desligado por custo), tentativas de acesso ao endpoint
  não ficam registradas.
- Do lado positivo: o pipeline funciona de qualquer runner sem infraestrutura extra, e o `kubectl`
  da máquina do dono funciona sem VPN ou bastion — o que a demonstração e o diagnóstico exigem.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Allowlist com os ranges de IP do GitHub Actions (`api.github.com/meta`) | São milhares de CIDRs, mudam sem aviso, e o limite do EKS é de 40 entradas em `public_access_cidrs` |
| Self-hosted runner dentro da VPC | Uma EC2 a mais (custo e limite de instâncias do lab), um instance profile que o lab não deixa criar, e manutenção do runner |
| Endpoint só privado + bastion ou SSM Session Manager | Bastion precisa de instance profile; Session Manager precisa de role; o `kubectl` do pipeline continuaria sem caminho |
| Endpoint só privado + VPN Client | Recurso pago e um endpoint de VPN a mais; o runner do GitHub não conecta em VPN |
| `public_access_cidrs` com o IP da máquina do dono | Resolveria o `kubectl` local, não o pipeline; e o IP residencial muda |

## Evolução

Fora do Learner Lab, o caminho é OIDC do GitHub Actions assumindo uma IAM role restrita ao cluster
**e** `public_access_cidrs` reduzido, ou um runner na VPC com endpoint só privado. Nenhum dos dois é
possível sem criar IAM.

## Evidências

| O quê | Onde |
|---|---|
| `vpc_config` com os dois acessos e a lista de CIDRs; `access_config` | `fiap-fase3-infra-k8s/eks.tf` |
| Variável `cluster_public_access_cidrs` com o motivo no `description` | `fiap-fase3-infra-k8s/variables.tf` |
| `update-kubeconfig` e `kubectl apply` a partir do runner hospedado | `fiap-fase3-app/.github/workflows/cd.yml` (steps `Configure kubeconfig`, `Apply Deployment, Service and HPA`) |
| Deploys pelo pipeline com ReplicaSet novo e imagem no SHA do merge | 2026-08-29, 2026-09-01, 2026-09-03 |
