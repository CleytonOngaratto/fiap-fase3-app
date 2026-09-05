<#
.SYNOPSIS
    Valida cada NRQL de k8s/newrelic/dashboard.json contra a conta e publica o dashboard.

.DESCRIPTION
    A ordem importa: primeiro VALIDA, depois publica. Um dashboard com query sintaticamente errada
    ou apontando para métrica que não existe fica verde na UI e vazio no painel — e num projeto de
    avaliação isso é pior que não ter o painel, porque parece pronto.

    O script separa dois resultados que a UI do New Relic mistura:
      - query INVÁLIDA  -> erro de NRQL. Bloqueia a publicação.
      - query SEM DADO  -> sintaxe ok, zero resultados. Só avisa: pode ser janela sem tráfego.

    A user key sai do SSM (`/fase3/newrelic/user-key`), nunca de argumento — valor em linha de
    comando aparece na process list. Ela é de natureza OPOSTA à license key: lê e administra a conta,
    então nunca entra no cluster.

.PARAMETER ValidateOnly
    Só valida, não publica. Use para conferir as queries sem tocar na conta.

.EXAMPLE
    .\scripts\newrelic-dashboard.ps1 -ValidateOnly
    .\scripts\newrelic-dashboard.ps1
#>

[CmdletBinding()]
param(
    [string]$Region = "us-east-1",
    [int]$AccountId = 8439524,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Continue"

function Write-Head($t) { Write-Host "`n=== $t ===" -ForegroundColor Cyan }
function Write-Ok($t) { Write-Host "  [OK]    $t" -ForegroundColor Green }
function Write-Empty($t) { Write-Host "  [VAZIO] $t" -ForegroundColor Yellow }
function Write-Bad($t) { Write-Host "  [ERRO]  $t" -ForegroundColor Red }

function Invoke-Probe {
    param([Parameter(Mandatory)][string]$Exe, [Parameter(Mandatory)][string[]]$ProbeArgs)
    $ErrorActionPreference = "Continue"
    $out = & $Exe @ProbeArgs 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return $out
}

# PS 5.1 negocia TLS 1.0 por default, e a api.newrelic.com recusa. Sem isto o erro vem como
# "A conexão subjacente foi fechada", que parece rede.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$repoRoot = Split-Path $PSScriptRoot -Parent
$dashboardPath = Join-Path $repoRoot "k8s/newrelic/dashboard.json"
if (-not (Test-Path $dashboardPath)) { Write-Bad "nao achei $dashboardPath"; exit 1 }

Write-Head "User key (SSM)"
$key = ((Invoke-Probe -Exe "aws" -ProbeArgs @(
            "ssm", "get-parameter", "--name", "/fase3/newrelic/user-key", "--with-decryption",
            "--region", $Region, "--query", "Parameter.Value", "--output", "text")) -join "") -replace "`r|`n", ""
if (-not $key) {
    Write-Bad "/fase3/newrelic/user-key ausente ou ilegivel."
    Write-Host "         New Relic -> API keys -> Create a key -> tipo USER (NRAK-, 32 chars)." -ForegroundColor Red
    Write-Host "         Depois: aws ssm put-parameter --name /fase3/newrelic/user-key --type SecureString --value file://<arquivo> --overwrite" -ForegroundColor Red
    exit 1
}
if (-not $key.StartsWith("NRAK-")) {
    Write-Bad "a chave em /fase3/newrelic/user-key nao comeca com NRAK- — provavelmente e uma ingest key, que so ESCREVE."
    exit 1
}
Write-Ok "user key lida ($($key.Length) chars, prefixo $($key.Substring(0,5)))"

function Invoke-NerdGraph {
    param([Parameter(Mandatory)][string]$Query)
    $body = @{ query = $Query } | ConvertTo-Json -Depth 20 -Compress
    # Body em BYTES UTF-8, nao string: o Invoke-RestMethod do PS 5.1 serializa string como ASCII, e
    # um acento dentro do NRQL (ex.: AS 'requisicoes') chega corrompido -> JSON invalido -> HTTP 400,
    # que parece erro de query e nao de encoding. So a query COM acento falha, o que engana mais.
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    try {
        return Invoke-RestMethod -Method Post -Uri "https://api.newrelic.com/graphql" `
            -Headers @{ "API-Key" = $key } `
            -ContentType "application/json; charset=utf-8" `
            -Body $bytes -TimeoutSec 60
    }
    catch {
        return [pscustomobject]@{ transportError = $_.Exception.Message }
    }
}

# `Get-Content` sem -Encoding le ANSI no PS 5.1 quando o arquivo nao tem BOM, e os acentos dos
# titulos chegariam corrompidos ao New Relic. Le explicito em UTF-8.
$dashboard = [System.IO.File]::ReadAllText($dashboardPath, [System.Text.UTF8Encoding]::new($false)) | ConvertFrom-Json

$queries = @()
foreach ($page in $dashboard.pages) {
    foreach ($w in $page.widgets) {
        foreach ($q in $w.rawConfiguration.nrqlQueries) {
            $queries += [pscustomobject]@{ Page = $page.name; Widget = $w.title; Nrql = $q.query }
        }
    }
}

Write-Head "Validando $($queries.Count) NRQL contra a conta $AccountId"
$invalid = 0
$empty = 0
foreach ($q in $queries) {
    $escaped = ($q.Nrql -replace '\\', '\\' -replace '"', '\"')
    $gql = "{ actor { account(id: $AccountId) { nrql(query: `"$escaped`") { results } } } }"
    $res = Invoke-NerdGraph $gql

    if ($res.transportError) { Write-Bad "$($q.Widget) — transporte: $($res.transportError)"; $invalid++; continue }
    if ($res.errors) { Write-Bad "$($q.Widget) — $($res.errors[0].message)"; $invalid++; continue }

    $results = @($res.data.actor.account.nrql.results)
    if ($results.Count -eq 0) { Write-Empty "$($q.Widget) — nenhuma linha"; $empty++; continue }

    # Contar linhas NAO e validar. Uma query pode devolver uma linha com valor 0 e o painel dizer
    # "0 pods up" com 4 pods rodando — foi o que aconteceu com um WHERE que filtrava pelo VALOR da
    # metrica, que no New Relic nao e atributo. Aqui se olha o CONTEUDO.
    $numbers = foreach ($row in $results) {
        foreach ($prop in $row.PSObject.Properties) {
            if ($prop.Name -notin @("facet", "beginTimeSeconds", "endTimeSeconds", "timestamp") -and
                $prop.Value -is [ValueType] -and $prop.Value -isnot [bool]) { [double]$prop.Value }
        }
    }
    if ($numbers -and (@($numbers | Where-Object { $_ -ne 0 }).Count -eq 0)) {
        # Heuristica, nao veredito: zero pode ser o estado SAUDAVEL (painel de erro 5xx sem erro) ou
        # uma query errada (foi assim que "0 pods up" passou com 4 pods rodando). O script nao sabe
        # a diferenca, entao avisa e deixa a decisao para quem le — sem bloquear a publicacao.
        Write-Empty "$($q.Widget) — $($results.Count) linha(s), todos os valores ZERO. Confira se e o esperado."
        $empty++
    }
    else { Write-Ok "$($q.Widget) ($($results.Count) resultado(s))" }
}

Write-Host ""
if ($invalid -gt 0) {
    Write-Bad "$invalid query(s) INVALIDA(S) — nao vou publicar. Corrija o dashboard.json."
    exit 1
}
if ($empty -gt 0) {
    Write-Empty "$empty query(s) sem valor diferente de zero. Sintaxe ok — verifique CADA uma:"
    Write-Host "         - painel de ERRO em zero e o estado saudavel, nao um defeito;" -ForegroundColor Yellow
    Write-Host "         - painel de VOLUME em zero costuma ser janela sem trafego (counter do Prometheus" -ForegroundColor Yellow
    Write-Host "           e delta entre scrapes, e cada serie nova perde o 1o incremento);" -ForegroundColor Yellow
    Write-Host "         - painel de ESTADO em zero (pods, replicas) e quase sempre query errada." -ForegroundColor Yellow
}

if ($ValidateOnly) { Write-Host "`n-ValidateOnly: nada publicado." -ForegroundColor Cyan; exit 0 }

Write-Head "Publicando"

$search = Invoke-NerdGraph "{ actor { entitySearch(query: `"name = '$($dashboard.name)' AND type = 'DASHBOARD'`") { results { entities { guid name } } } } }"
$existing = $search.data.actor.entitySearch.results.entities | Where-Object { $_.name -eq $dashboard.name } | Select-Object -First 1

# O JSON do arquivo e o input da mutation: converter para GraphQL literal evita manter duas
# representacoes do mesmo dashboard em sincronia manual.
function ConvertTo-GraphQLLiteral($obj) {
    if ($null -eq $obj) { return "null" }
    if ($obj -is [bool]) { return $obj.ToString().ToLower() }
    if ($obj -is [int] -or $obj -is [long] -or $obj -is [double] -or $obj -is [decimal]) { return "$obj" }
    if ($obj -is [string]) {
        # PUBLIC_READ_WRITE e enum no schema: enum nao leva aspas.
        if ($obj -cmatch '^[A-Z][A-Z_]+$' -and $obj.Length -gt 3) { return $obj }
        return (ConvertTo-Json $obj -Compress)
    }
    if ($obj -is [array] -or $obj -is [System.Collections.IEnumerable]) {
        return "[" + (($obj | ForEach-Object { ConvertTo-GraphQLLiteral $_ }) -join ",") + "]"
    }
    $fields = foreach ($p in $obj.PSObject.Properties) { "$($p.Name):$(ConvertTo-GraphQLLiteral $p.Value)" }
    return "{" + ($fields -join ",") + "}"
}

$input = ConvertTo-GraphQLLiteral $dashboard

if ($existing) {
    Write-Host "  atualizando $($existing.guid)" -ForegroundColor DarkGray
    $mutation = "mutation { dashboardUpdate(guid: `"$($existing.guid)`", dashboard: $input) { entityResult { guid name } errors { description type } } }"
    $res = Invoke-NerdGraph $mutation
    $payload = $res.data.dashboardUpdate
}
else {
    $mutation = "mutation { dashboardCreate(accountId: $AccountId, dashboard: $input) { entityResult { guid name } errors { description type } } }"
    $res = Invoke-NerdGraph $mutation
    $payload = $res.data.dashboardCreate
}

if ($res.transportError) { Write-Bad "transporte: $($res.transportError)"; exit 1 }
if ($res.errors) { Write-Bad $res.errors[0].message; exit 1 }
if ($payload.errors) {
    foreach ($e in $payload.errors) { Write-Bad "$($e.type): $($e.description)" }
    exit 1
}

$guid = $payload.entityResult.guid
Write-Ok "dashboard '$($payload.entityResult.name)' publicado"
Write-Host ""
Write-Host "  GUID: $guid" -ForegroundColor DarkGray
Write-Host "  Abra em: https://one.newrelic.com/dashboards/detail/$guid" -ForegroundColor Cyan
