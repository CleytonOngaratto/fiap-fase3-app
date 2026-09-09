# ADR-002 — Um único ambiente (produção)

| | |
|---|---|
| **Status** | Aceita e implementada |
| **Data** | 2026-07-28 (decisão) · 2026-08-04 (reconfirmada) |
| **Afeta** | Os quatro repositórios; pipelines; API Gateway (stage única) |

## Contexto

O enunciado do Tech Challenge menciona "deploy das branches de homologação e produção". Em
esclarecimento dos professores, esse trecho é um **erro conhecido do enunciado** — da mesma
categoria do "vale 60 % da nota", que na verdade vale 90 % —, e o que se avalia é um ambiente
funcional com CI/CD por repositório e `main` protegida.

Ao mesmo tempo, a nuvem é o AWS Academy Learner Lab: crédito fixo de US$ 50, control plane do EKS
faturando ~US$ 0,10/h mesmo com o lab desligado, e a infraestrutura inteira destruída entre sessões.

## Decisão

**Um só ambiente, tratado como produção.** Um cluster EKS, um RDS, uma Lambda, uma stage
(`$default`) no API Gateway. Sem branch de homologação. O fluxo é: PR → validação (`plan`, testes,
Trivy); merge na `main` → deploy.

Registrar isto é o ponto: sem a ADR, a ausência de homologação pareceria omissão em vez de decisão.

## Consequências

Boas:

- Metade do custo em relação a dois ambientes (o control plane do EKS sozinho seria mais US$ 2,40
  por dia ligado).
- Zero drift entre ambientes: o que a banca vê é o que o pipeline aplicou na `main`.
- Stage `$default` do Gateway sem prefixo: o path que o cliente chama é o que a aplicação recebe.

Ruins, aceitas:

- **Merge na `main` é deploy em produção.** Não há canário nem janela de homologação; a validação
  antes do merge é o que existe (`mvn clean verify` com 432 testes e gate de cobertura, `terraform
  plan` no PR, Trivy). Um merge ruim é revertido por outro PR.
- **O `cd.yml` e os `terraform.yml` falham fora da sessão do lab**, por credencial expirada. É
  esperado, e é por isso que nenhum deles é *status check* obrigatório no ruleset — travaria todo
  merge fora do horário do laboratório. O `paths-ignore: **.md` do `cd.yml` evita que merge só de
  documentação pinte a `main` de vermelho.
- Não há como ensaiar uma migration destrutiva contra dados reais antes de aplicá-la.
- A demonstração de autoescala, alertas e dashboard acontece **no** ambiente de produção, com carga
  sintética.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Dois clusters (homologação e produção) | Dobra o control plane, os nós, o NAT e o RDS: ~US$ 0,50/h. Com o crédito do lab, isso reduz à metade o número de sessões disponíveis para validar e gravar |
| Dois namespaces no mesmo cluster | Barato, mas não é isolamento: mesmo RDS (ou um segundo RDS com a mesma senha no mesmo state), mesmos nós, mesmo teto de réplicas. Simularia homologação sem entregar o que homologação promete |
| Workspaces do Terraform (`homolog`/`prod`) nos três repositórios de infraestrutura | Duplica cada recurso pago; e o contrato SSM precisaria de um prefixo por ambiente em todos os leitores, inclusive Lambda e pipeline |
| Stage `homolog` no API Gateway apontando para o mesmo cluster | Só muda a URL; a aplicação e o banco por trás seriam os mesmos |

## Evidências

| O quê | Onde |
|---|---|
| Stage única com `auto_deploy` | `fiap-fase3-auth-serverless/apigateway.tf` |
| `multi_az = false` justificado por ambiente único | `fiap-fase3-infra-db/variables.tf` |
| Gatilhos: `plan`/testes no PR, `apply`/deploy no push na `main`, `paths-ignore` de `**.md` | `.github/workflows/terraform.yml` nos repositórios 1, 2 e 3; `cd.yml` e `maven-ci.yml` no 4 |
| Ruleset da `main` sem status check obrigatório, nos quatro repositórios | configurado na publicação de cada repositório (agosto de 2026); o do repositório 1 conferido em 2026-09-04 |
