<#
.SYNOPSIS
    Instala a integração de infraestrutura Kubernetes do New Relic (chart nri-bundle) no EKS.

.DESCRIPTION
    O agente Java reporta a APLICAÇÃO; este bundle reporta o CLUSTER (nós, pods, deployments,
    eventos). É um release Helm separado: o cd.yml não o conhece, e a infraestrutura é destruída
    entre sessões, então isto roda a cada sessão — não uma vez.

    Valores em k8s/newrelic/values.yaml. Detalhes no README, em Observabilidade.

.PARAMETER Uninstall
    Remove o release e o namespace, para iterar sem recriar o cluster.
#>
[CmdletBinding()]
param(
    [string]$Region       = "us-east-1",
    [string]$ChartVersion = "8.0.22",
    [switch]$Uninstall
)

$ErrorActionPreference = "Stop"
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$Namespace  = "newrelic"
$Release    = "newrelic-bundle"
$SecretName = "newrelic-license"
$Root       = Split-Path -Parent $PSScriptRoot
$ValuesFile = Join-Path $Root "k8s\newrelic\values.yaml"

function Write-Step { param([string]$Message) Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Write-Note { param([string]$Message) Write-Host "    $Message" -ForegroundColor DarkGray }
function Write-Alert { param([string]$Message) Write-Host "    [aviso] $Message" -ForegroundColor Yellow }

# No PS 5.1, redirecionar o stderr de um executável nativo vira NativeCommandError — e com
# $ErrorActionPreference = 'Stop' isso derruba o script numa pergunta legítima. Igual ao deploy.ps1.
function Invoke-Probe {
    param([Parameter(Mandatory)][string]$Exe, [Parameter(Mandatory)][string[]]$ProbeArgs)
    $ErrorActionPreference = "Continue"
    $out = & $Exe @ProbeArgs 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return $out
}

function Invoke-Checked {
    param([Parameter(Mandatory)][string]$Exe, [Parameter(Mandatory)][string[]]$CArgs)
    $out = & $Exe @CArgs
    if ($LASTEXITCODE -ne 0) { throw "$Exe $($CArgs -join ' ') falhou (exit $LASTEXITCODE)" }
    return $out
}

function Get-SsmValue {
    param([Parameter(Mandatory)][string]$Name, [string]$Source, [switch]$Secure)
    $cliArgs = @("ssm", "get-parameter", "--name", $Name,
                 "--query", "Parameter.Value", "--output", "text", "--region", $Region)
    if ($Secure) { $cliArgs += "--with-decryption" }
    $value = (Invoke-Probe -Exe "aws" -ProbeArgs $cliArgs) -join "`n"
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "SSM: parâmetro '$Name' ausente ou vazio.`n    Quem publica: $Source"
    }
    return $value.Trim()
}


Write-Step "Pré-requisitos"

$helm = Invoke-Probe -Exe "helm" -ProbeArgs @("version", "--short")
if (-not $helm) {
    throw @"
helm não encontrado no PATH.

    Esta máquina não tem winget/choco/scoop, então a instalação é manual — baixe o zip oficial,
    confira o SHA256 e ponha o binário no PATH:

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    `$ver = "v3.21.4"; `$zip = "helm-`$ver-windows-amd64.zip"
    Invoke-WebRequest "https://get.helm.sh/`$zip"           -OutFile "`$env:TEMP\`$zip"        -UseBasicParsing
    Invoke-WebRequest "https://get.helm.sh/`$zip.sha256sum" -OutFile "`$env:TEMP\`$zip.sha256" -UseBasicParsing
    `$exp = ((Get-Content "`$env:TEMP\`$zip.sha256" -Raw).Trim() -split '\s+')[0]
    if (`$exp -ne (Get-FileHash "`$env:TEMP\`$zip" -Algorithm SHA256).Hash.ToLower()) { throw "SHA256 não confere" }
    Expand-Archive "`$env:TEMP\`$zip" -DestinationPath "`$env:TEMP\helm-x" -Force
    New-Item -ItemType Directory -Force "`$env:LOCALAPPDATA\Programs\helm" | Out-Null
    Copy-Item "`$env:TEMP\helm-x\windows-amd64\helm.exe" "`$env:LOCALAPPDATA\Programs\helm\"
    `$env:PATH = "`$env:LOCALAPPDATA\Programs\helm;`$env:PATH"

    Invoke-WebRequest e não o curl do Git Bash: o IWR usa o repositório de certificados do Windows,
    onde a raiz do antivírus está confiada; o curl do Git traz CA bundle próprio e leva x509.
"@
}
Write-Note "helm: $helm"

if (-not (Test-Path $ValuesFile)) { throw "values não encontrado: $ValuesFile" }

$who = Invoke-Probe -Exe "aws" -ProbeArgs @("sts", "get-caller-identity", "--query", "Arn", "--output", "text")
if (-not $who) {
    throw "Credenciais da AWS inválidas ou expiradas (a sessão do Learner Lab dura ~4h).`n" +
          "    Recole o 'AWS Details' do painel em ~/.aws/credentials."
}
Write-Note "identidade: $who"

$cluster = Get-SsmValue "/fase3/eks/cluster-name" -Source "repo 2 (fiap-fase3-infra-k8s)"

$context = Invoke-Probe -Exe "kubectl" -ProbeArgs @("config", "current-context")
if ([string]::IsNullOrWhiteSpace($context) -or $context -notlike "*$cluster*") {
    throw "O contexto do kubectl ('$context') não é o cluster do contrato ('$cluster').`n" +
          "    Rode: aws eks update-kubeconfig --name $cluster --region $Region`n" +
          "    (obrigatório a cada recriação: o endpoint muda, o ARN do contexto não — e o erro" +
          " que aparece é 'no such host', que engana.)"
}
Write-Note "contexto: $context"


if ($Uninstall) {
    Write-Step "Removendo o release"
    Invoke-Probe -Exe "helm" -ProbeArgs @("uninstall", $Release, "-n", $Namespace) | Out-Null
    Invoke-Probe -Exe "kubectl" -ProbeArgs @("delete", "namespace", $Namespace, "--wait=false") | Out-Null
    Write-Host "`n  Release '$Release' e namespace '$Namespace' removidos." -ForegroundColor Green
    return
}


Write-Step "Namespace e license key (do SSM)"

$licenseKey = Get-SsmValue "/fase3/newrelic/license-key" -Secure `
    -Source "bootstrap manual do dono; nenhum Terraform cria este parâmetro"

$nsYaml = & kubectl create namespace $Namespace --dry-run=client -o yaml
$nsYaml | & kubectl apply -f - | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Falha ao criar/atualizar o namespace '$Namespace'" }

# Por stdin, e não por `--set` do helm nem por values file: argumento de linha de comando aparece
# na process list, e um values com segredo seria uma segunda cópia da chave em disco.
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($licenseKey))
$secretYaml = @(
    "apiVersion: v1",
    "kind: Secret",
    "metadata:",
    "  name: $SecretName",
    "  namespace: $Namespace",
    "type: Opaque",
    "data:",
    "  licenseKey: $b64"
) -join "`n"
$secretYaml | & kubectl apply -f - | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Falha ao aplicar o Secret '$SecretName'" }
Write-Note "namespace/$Namespace e secret/$SecretName (1 chave)"


Write-Step "Instalando o chart nri-bundle $ChartVersion"

Invoke-Checked -Exe "helm" -CArgs @("repo", "add", "newrelic", "https://helm-charts.newrelic.com",
                                    "--force-update") | Out-Null
Invoke-Checked -Exe "helm" -CArgs @("repo", "update", "newrelic") | Out-Null

Invoke-Checked -Exe "helm" -CArgs @(
    "upgrade", "--install", $Release, "newrelic/nri-bundle",
    "--version", $ChartVersion,
    "-n", $Namespace,
    "-f", $ValuesFile,
    "--set", "global.cluster=$cluster",
    "--wait", "--timeout", "10m"
)


Write-Step "Componentes"
Invoke-Checked -Exe "kubectl" -CArgs @("-n", $Namespace, "get", "daemonset,deployment,pods", "-o", "wide")

# O logging tem que estar AUSENTE do cluster, não só "desligado no values".
$logging = Invoke-Probe -Exe "kubectl" -ProbeArgs @(
    "-n", $Namespace, "get", "daemonset", "-o", "jsonpath={.items[*].metadata.name}")
if ($logging -match "logging|fluent") {
    Write-Alert "há um DaemonSet de logging no namespace: '$logging'."
    Write-Alert "os logs da aplicação vão chegar DUPLICADOS ao New Relic. Confira o values.yaml."
} else {
    Write-Note "nenhum DaemonSet de logging — sem duplicação de log da aplicação"
}


# Por nó, e não pelo total do cluster: os Deployments do bundle caem todos no mesmo nó, e é ele
# que passa a limitar quantas réplicas da aplicação cabem.
Write-Step "Memória por nó (requests — é o que o agendador enxerga)"

$appRequest = Invoke-Probe -Exe "kubectl" -ProbeArgs @(
    "-n", "car-workshop", "get", "deploy", "car-workshop-api",
    "-o", "jsonpath={.spec.template.spec.containers[0].resources.requests.memory}")

function ConvertTo-Mi {
    param([string]$v)
    if (-not $v) { return 0.0 }
    switch -regex ($v) {
        '^(\d+(\.\d+)?)Mi$' { return [double]$matches[1] }
        '^(\d+(\.\d+)?)Gi$' { return [double]$matches[1] * 1024 }
        '^(\d+(\.\d+)?)Ki$' { return [double]$matches[1] / 1024 }
        # M e G do Kubernetes sao decimais. O sufixo 1MB do PowerShell e binario e daria 1:1 com
        # Mi — os requests do chart vem em "150M" e a conta sairia 7% otimista.
        '^(\d+(\.\d+)?)M$'  { return [double]$matches[1] * 1000000 / 1048576 }
        '^(\d+(\.\d+)?)G$'  { return [double]$matches[1] * 1000000000 / 1048576 }
        '^(\d+)$'           { return [double]$matches[1] / 1048576 }
        default             { return 0.0 }
    }
}

$appMi = ConvertTo-Mi $appRequest
$nodes = (Invoke-Checked -Exe "kubectl" -CArgs @("get", "nodes", "-o", "jsonpath={.items[*].metadata.name}")) -split '\s+'
$totalFits = 0

foreach ($node in ($nodes | Where-Object { $_ })) {
    $allocMi = ConvertTo-Mi (Invoke-Checked -Exe "kubectl" -CArgs @(
        "get", "node", $node, "-o", "jsonpath={.status.allocatable.memory}"))

    $spec = Invoke-Checked -Exe "kubectl" -CArgs @(
        "get", "pods", "--all-namespaces", "--field-selector", "spec.nodeName=$node,status.phase=Running",
        "-o", "jsonpath={range .items[*]}{.metadata.namespace}{'/'}{.metadata.labels.app}{'|'}{range .spec.containers[*]}{.resources.requests.memory}{','}{end}{'\n'}{end}")

    $baseMi = 0.0; $appPods = 0; $nrMi = 0.0
    foreach ($line in ($spec -split "`n" | Where-Object { $_ -match '\|' })) {
        $ns, $rest = $line -split '\|', 2
        $podMi = 0.0
        foreach ($m in ($rest -split ',' | Where-Object { $_ })) { $podMi += ConvertTo-Mi $m }
        if ($ns -like "car-workshop/*") { $appPods++ } else { $baseMi += $podMi }
        if ($ns -like "$Namespace/*")   { $nrMi += $podMi }
    }

    $freeForApp = $allocMi - $baseMi
    $fits = [math]::Floor($freeForApp / [math]::Max($appMi, 1))

    Write-Host ("`n  {0}" -f $node)
    Write-Note ("allocatable ............... {0,7:N0} Mi" -f $allocMi)
    Write-Note ("requests nao-app .......... {0,7:N0} Mi   (dos quais New Relic: {1:N0} Mi)" -f $baseMi, $nrMi)
    Write-Note ("sobra para a aplicacao .... {0,7:N0} Mi" -f $freeForApp)
    Write-Note ("cabem ..................... {0,7:N0} replicas de {1:N0} Mi  (agora: {2})" -f $fits, $appMi, $appPods)
    $totalFits += $fits
}

Write-Host ""
Write-Host "  Teto alcancavel pelo HPA com este bundle instalado: $totalFits replicas." -ForegroundColor Yellow
Write-Note "se for MENOR que o maxReplicas do k8s/application/hpa.yaml, o HPA vai pedir replicas"
Write-Note "que ficam Pending."

Write-Host "`n  Verificacao no painel do New Relic:" -ForegroundColor Green
Write-Host @"

    FROM K8sNodeSample       SELECT uniqueCount(nodeName) WHERE clusterName = '$cluster' SINCE 10 minutes ago
    FROM K8sPodSample        SELECT count(*) WHERE clusterName = '$cluster' FACET namespaceName SINCE 10 minutes ago
    FROM K8sDeploymentSample SELECT latest(podsDesired), latest(podsReady) WHERE deploymentName = 'car-workshop-api'
    FROM InfrastructureEvent SELECT count(*) WHERE category = 'kubernetes' SINCE 30 minutes ago

  E o que NAO pode acontecer (log duplicado) — so 'logs.APM' pode aparecer:

    FROM Log SELECT count(*) WHERE entity.name = 'car-workshop-api' FACET newrelic.source SINCE 30 minutes ago
"@
