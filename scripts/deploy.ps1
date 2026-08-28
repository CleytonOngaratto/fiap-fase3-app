<#
.SYNOPSIS
    Deploy da Car Workshop API no EKS.

.DESCRIPTION
    Lê o contrato /fase3/* do SSM, gera os três Secrets do namespace e aplica k8s/application/.

    Os Secrets são regerados a todo deploy de propósito: a senha do RDS muda a cada recriação da
    infraestrutura de banco, e um Secret criado "uma vez" faz a app subir e falhar ao conectar com a
    credencial da sessão anterior.

    Não builda nem publica a imagem, e não publica /fase3/eks/lb-dns no SSM — só imprime o DNS.

.PARAMETER Tag
    Tag da imagem no ECR. Default: SHA curto do commit (+ "-dirty" se houver mudança não commitada).

    Não use uma tag fixa tipo "latest": se o spec do Deployment não muda, o `kubectl apply` é no-op —
    nenhum pod é recriado e o `rollout status` devolve "successfully rolled out" na hora, referindo-se
    ao deploy anterior. O resultado é validar a imagem velha achando que deployou.

.PARAMETER Restart
    Força `kubectl rollout restart` depois do apply, para quando a imagem foi republicada sob a
    mesma tag.

.EXAMPLE
    $tag = git rev-parse --short HEAD
    docker build -t "${ecr}:${tag}" .
    docker push "${ecr}:${tag}"
    .\scripts\deploy.ps1 -Tag $tag
#>
[CmdletBinding()]
param(
    [string]$Tag,
    [string]$Region = "us-east-1",
    [switch]$Restart
)

$ErrorActionPreference = "Stop"
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$Namespace = "car-workshop"
$AppName   = "car-workshop-api"
$Root      = Split-Path -Parent $PSScriptRoot
$Manifests = Join-Path $Root "k8s\application"

function Write-Step { param([string]$Message) Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Write-Note { param([string]$Message) Write-Host "    $Message" -ForegroundColor DarkGray }
function Write-Alert { param([string]$Message) Write-Host "    [aviso] $Message" -ForegroundColor Yellow }

function Invoke-Kubectl {
    param([Parameter(Mandatory)][string[]]$KArgs)
    $out = & kubectl @KArgs
    if ($LASTEXITCODE -ne 0) { throw "kubectl $($KArgs -join ' ') falhou (exit $LASTEXITCODE)" }
    return $out
}

# No PS 5.1, redirecionar o stderr de um executável nativo embrulha cada linha num ErrorRecord
# (NativeCommandError) — e com $ErrorActionPreference = 'Stop' isso vira exceção mesmo numa pergunta
# legítima, como "existe este Deployment?" antes do primeiro deploy. O 'Continue' local mantém o erro
# não-terminante e o 2>$null o descarta.
function Invoke-Probe {
    param([Parameter(Mandatory)][string]$Exe, [Parameter(Mandatory)][string[]]$ProbeArgs)
    $ErrorActionPreference = "Continue"
    $out = & $Exe @ProbeArgs 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return $out
}

function Get-SsmValue {
    param(
        [Parameter(Mandatory)][string]$Name,
        [string]$Source,
        [switch]$Secure,
        [switch]$Optional
    )
    $cliArgs = @("ssm", "get-parameter", "--name", $Name,
                 "--query", "Parameter.Value", "--output", "text", "--region", $Region)
    if ($Secure) { $cliArgs += "--with-decryption" }

    # PEM volta do CLI como array de linhas; o -join reconstrói com LF, que é como o SSM guarda.
    $value = (Invoke-Probe -Exe "aws" -ProbeArgs $cliArgs) -join "`n"

    if ([string]::IsNullOrWhiteSpace($value)) {
        if ($Optional) { return $null }
        throw "SSM: parâmetro '$Name' ausente ou vazio.`n    Quem publica: $Source"
    }
    return $value
}

# Base64 montado aqui, e não `kubectl create secret --from-literal`: a senha do RDS usa
# "*()-_=+[]{}" e a chave privada é multilinha — como argumento nativo no PS 5.1 os dois são
# manglados, e o sintoma vira "erro de autenticação" ou "PEM inválido". A chave nunca toca o disco.
function New-SecretYaml {
    param([Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][hashtable]$Data)
    $lines = @(
        "apiVersion: v1",
        "kind: Secret",
        "metadata:",
        "  name: $Name",
        "  namespace: $Namespace",
        "  labels:",
        "    app: $AppName",
        "type: Opaque",
        "data:"
    )
    foreach ($key in ($Data.Keys | Sort-Object)) {
        $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$Data[$key]))
        $lines += "  ${key}: $b64"
    }
    return ($lines -join "`n")
}

function Set-Secret {
    param([Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][hashtable]$Data)
    $yaml = New-SecretYaml -Name $Name -Data $Data
    $yaml | & kubectl apply -f - | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Falha ao aplicar o Secret '$Name' (exit $LASTEXITCODE)" }
    Write-Note "$Name — $($Data.Count) chave(s)"
}


Write-Step "Credenciais e contrato SSM"

$who = Invoke-Probe -Exe "aws" -ProbeArgs @("sts", "get-caller-identity", "--query", "Arn", "--output", "text")
if (-not $who) {
    throw "Credenciais da AWS inválidas ou expiradas (a sessão do Learner Lab dura ~4h).`n" +
          "    Recole o 'AWS Details' do painel em ~/.aws/credentials."
}
Write-Note "identidade: $who"

$repo2 = "repo 2 (fiap-fase3-infra-k8s) — rode o terraform apply lá primeiro"
$repo3 = "repo 3 (fiap-fase3-infra-db) — os parâmetros só saem quando a instância fica 'available'"
$boot  = "bootstrap manual do dono; sobrevive ao destroy, não é criado por Terraform nenhum"

$ecrRepo = Get-SsmValue "/fase3/ecr/repo-url"     -Source $repo2
$cluster = Get-SsmValue "/fase3/eks/cluster-name" -Source $repo2

$dbHost = Get-SsmValue "/fase3/rds/endpoint" -Source $repo3
$dbPort = Get-SsmValue "/fase3/rds/port"     -Source $repo3
$dbName = Get-SsmValue "/fase3/rds/db-name"  -Source $repo3
$dbUser = Get-SsmValue "/fase3/rds/username" -Source $repo3
$dbPass = Get-SsmValue "/fase3/rds/password" -Source $repo3 -Secure

$jwtPrivate = Get-SsmValue "/fase3/jwt/private-key" -Source $boot -Secure
$jwtPublic  = Get-SsmValue "/fase3/jwt/public-key"  -Source $boot -Secure

# Obrigatório: sem a var a app sobe e concatena a string literal "null" no hash de senha.
$secretKeyHelp = @"
$boot.
    Publicar UMA vez (e nunca rotacionar: o valor entra no hash e invalida as senhas de admin
    já gravadas). Em uma linha:
    aws ssm put-parameter --name /fase3/app/secret-key --type SecureString --region $Region --value ([Convert]::ToBase64String([byte[]](1..32 | ForEach-Object { Get-Random -Maximum 256 })))
"@
$appSecretKey = Get-SsmValue "/fase3/app/secret-key" -Secure -Source $secretKeyHelp

# Opcional: sem a license key o agente não sobe e a app funciona igual.
$newRelicKey = Get-SsmValue "/fase3/newrelic/license-key" -Secure -Optional
if (-not $newRelicKey) {
    Write-Alert "/fase3/newrelic/license-key ausente — a app sobe sem o agente (sem APM no New Relic)."
}

# O -join acima não devolve a última quebra, e o PEM precisa terminar em newline.
foreach ($name in @("jwtPrivate", "jwtPublic")) {
    $pem = (Get-Variable $name).Value
    if (-not $pem.EndsWith("`n")) { Set-Variable $name -Value ($pem + "`n") }
}


Write-Step "Contexto do kubectl"

$context = Invoke-Probe -Exe "kubectl" -ProbeArgs @("config", "current-context")
if ([string]::IsNullOrWhiteSpace($context)) {
    throw "kubectl sem contexto ativo.`n    Rode: aws eks update-kubeconfig --name $cluster --region $Region"
}
if ($context -notlike "*$cluster*") {
    throw "O contexto atual do kubectl ('$context') não é o cluster do contrato ('$cluster').`n" +
          "    Rode: aws eks update-kubeconfig --name $cluster --region $Region"
}
Write-Note "contexto: $context"


if (-not $Tag) {
    $sha = Invoke-Probe -Exe "git" -ProbeArgs @("-C", $Root, "rev-parse", "--short", "HEAD")
    if (-not [string]::IsNullOrWhiteSpace($sha)) {
        $dirty = Invoke-Probe -Exe "git" -ProbeArgs @("-C", $Root, "status", "--porcelain")
        $Tag = if ([string]::IsNullOrWhiteSpace(($dirty -join ""))) { "$sha" } else { "$sha-dirty" }
    } else {
        $Tag = "local-" + (Get-Date -Format "yyyyMMdd-HHmmss")
    }
}
$image = "${ecrRepo}:${Tag}"

Write-Step "Imagem"
Write-Note $image

# A `generation` sobe a cada mudança de spec, então comparar antes/depois diz se o apply mudou
# alguma coisa. Comparar só a imagem daria falso alarme quando a mudança está em outro campo.
$generationBefore = Invoke-Probe -Exe "kubectl" -ProbeArgs @(
    "-n", $Namespace, "get", "deploy", $AppName, "-o", "jsonpath={.metadata.generation}")


# A ordem importa duas vezes: `kubectl apply -f <dir>` processa em ordem alfabética, e o
# namespace.yaml viria depois dos objetos que dependem dele; e o ConfigMap precisa existir antes do
# Deployment que o referencia em envFrom, senão o pod fica em CreateContainerConfigError.
Write-Step "Namespace e ConfigMap"
Invoke-Kubectl @("apply", "-f", (Join-Path $Manifests "namespace.yaml")) | Out-Null
Invoke-Kubectl @("apply", "-f", (Join-Path $Manifests "configmap.yaml")) | Out-Null
Write-Note "namespace/$Namespace e configmap/car-workshop-api-config"


Write-Step "Secrets (regerados do SSM)"

Set-Secret -Name "car-workshop-db" -Data @{
    DB_HOST     = $dbHost   # /fase3/rds/endpoint é o host puro, sem porta
    DB_PORT     = $dbPort
    DB_NAME     = $dbName
    DB_USERNAME = $dbUser
    DB_PASSWORD = $dbPass
}

$appData = @{ SECRET_KEY = $appSecretKey }
if ($newRelicKey) { $appData["NEW_RELIC_LICENSE_KEY"] = $newRelicKey }
Set-Secret -Name "car-workshop-app" -Data $appData

Set-Secret -Name "car-workshop-jwt" -Data @{
    "privateKey.pem" = $jwtPrivate
    "publicKey.pem"  = $jwtPublic
}


Write-Step "Deployment, Service e HPA"

$deploymentYaml = [IO.File]::ReadAllText((Join-Path $Manifests "deployment.yaml"))
if ($deploymentYaml -notmatch "__IMAGE__") {
    throw "deployment.yaml não tem o placeholder __IMAGE__ — alguém hardcodou a URL do ECR?"
}

$temp = Join-Path $env:TEMP "car-workshop-deployment-$PID.yaml"
try {
    [IO.File]::WriteAllText($temp, $deploymentYaml.Replace("__IMAGE__", $image),
                            (New-Object System.Text.UTF8Encoding($false)))
    Invoke-Kubectl @("apply", "-f", $temp) | Out-Null
} finally {
    Remove-Item $temp -Force -ErrorAction SilentlyContinue
}

Invoke-Kubectl @("apply", "-f", (Join-Path $Manifests "service.yaml")) | Out-Null
Invoke-Kubectl @("apply", "-f", (Join-Path $Manifests "hpa.yaml"))     | Out-Null

$generationAfter = Invoke-Probe -Exe "kubectl" -ProbeArgs @(
    "-n", $Namespace, "get", "deploy", $AppName, "-o", "jsonpath={.metadata.generation}")

if ($Restart) {
    Write-Note "rollout restart forçado (-Restart)"
    Invoke-Kubectl @("-n", $Namespace, "rollout", "restart", "deployment/$AppName") | Out-Null
} elseif ($generationBefore -and $generationBefore -eq $generationAfter) {
    Write-Alert "o spec do Deployment não mudou: o apply foi no-op e NENHUM pod novo foi criado."
    Write-Alert "o rollout status abaixo vai dizer 'successfully rolled out' na hora, referindo-se ao"
    Write-Alert "deploy ANTERIOR. Se você republicou a imagem sob a tag '$Tag', rode com -Restart."
}


# 600s: no primeiro deploy o pull é frio, são 2 réplicas, e a startupProbe sozinha tolera 150s.
Write-Step "Aguardando o rollout"
Invoke-Kubectl @("-n", $Namespace, "rollout", "status", "deployment/$AppName", "--timeout=600s")

Write-Step "Aguardando o DNS do LoadBalancer"
$lbHost = $null
$deadline = (Get-Date).AddMinutes(5)
while ((Get-Date) -lt $deadline) {
    $lbHost = Invoke-Probe -Exe "kubectl" -ProbeArgs @(
        "-n", $Namespace, "get", "svc", $AppName,
        "-o", "jsonpath={.status.loadBalancer.ingress[0].hostname}")
    if (-not [string]::IsNullOrWhiteSpace($lbHost)) { break }
    Start-Sleep -Seconds 10
}

if ([string]::IsNullOrWhiteSpace($lbHost)) {
    Write-Alert "o ELB ainda não reportou hostname. Acompanhe: kubectl -n $Namespace get svc $AppName -w"
    Write-Alert 'se travar em <pending>, o evento do "kubectl describe svc" mostra o AccessDenied.'
    exit 1
}

Write-Host "`n  Deploy concluído." -ForegroundColor Green
Write-Host "  imagem : $image"
Write-Host "  URL    : http://$lbHost/carworkshop/v1"
Write-Host @"

  Verificação (o DNS do ELB leva ~3 min para propagar depois de aparecer):

    curl.exe -i "http://$lbHost/carworkshop/v1/q/health/ready"
    kubectl -n $Namespace get pods,svc,hpa

  ATENCAO - antes do terraform destroy do repo 2:
    kubectl -n $Namespace delete svc $AppName
  O ELB deixa uma ENI na subnet e trava a destruicao da VPC.
"@
