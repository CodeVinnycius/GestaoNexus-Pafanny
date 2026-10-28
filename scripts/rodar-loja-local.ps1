<#
  Sobe a loja PAFANNY no seu computador, para testar ANTES de publicar.

  - Usa uma CÓPIA do banco e das fotos (em %TEMP%\pafanny-teste): o banco original
    em data\ e o site no ar nunca são tocados.
  - Senhas e chave de login são geradas na hora, só para este teste.
  - Porta padrão 8081 (a 8080 costuma estar ocupada por outro projeto).

  Uso (PowerShell, dentro da pasta EstoqueSpring):
    .\scripts\rodar-loja-local.ps1
    .\scripts\rodar-loja-local.ps1 -Porta 8090
    .\scripts\rodar-loja-local.ps1 -CopiarBancoDeNovo     # descarta a cópia antiga e copia o banco atual
  Depois abra http://localhost:8081 . Para parar: Ctrl+C nesta janela.
#>
param(
    [int]$Porta = 8081,
    [switch]$CopiarBancoDeNovo
)

# O script fica em scripts\; data\ e o pom.xml estão na pasta de cima.
Set-Location -Path (Split-Path $PSScriptRoot -Parent)
$copia = Join-Path $env:TEMP "pafanny-teste"

if ($CopiarBancoDeNovo -and (Test-Path $copia)) { Remove-Item -Recurse -Force $copia }
New-Item -ItemType Directory -Force $copia | Out-Null
if (-not (Test-Path (Join-Path $copia "estoque.mv.db"))) {
    Copy-Item "data\estoque.mv.db" $copia
    Write-Host "Banco copiado para $copia"
}
if (-not (Test-Path (Join-Path $copia "uploads"))) {
    Copy-Item "data\uploads" $copia -Recurse
    Write-Host "Fotos copiadas para $copia\uploads"
}

$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $mvn = Get-ChildItem "C:\Program Files\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mvn) { Write-Host "Maven não encontrado (nem no PATH nem no IntelliJ)." -ForegroundColor Red; exit 1 }

$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)

$env:SERVER_PORT        = "$Porta"
$env:DB_URL             = "jdbc:h2:file:$copia\estoque;DB_CLOSE_ON_EXIT=FALSE"
$env:APP_UPLOADS_DIR    = "$copia\uploads"
$env:APP_DATA_DIR       = $copia
$env:APP_JWT_SECRET     = [Convert]::ToBase64String($bytes)
$env:APP_ADMIN_SENHA    = "teste-local-" + [guid]::NewGuid()
$env:APP_LOJA_WHATSAPP  = "5511986032979"
$env:APP_LOJA_INSTAGRAM = "usepafanny"

Write-Host "Subindo a loja em http://localhost:$Porta ... (aguarde a linha 'Started')" -ForegroundColor Cyan
& $mvn -o -q spring-boot:run "-Dspring-boot.run.addResources=true"
