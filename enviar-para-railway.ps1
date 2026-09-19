<#
  ==============================================================
  enviar-para-railway.ps1

  Copia o banco e as fotos do sistema LOCAL para o servidor no ar (Railway).

  O que faz:
    1. Entra como admin no sistema local e baixa um backup consistente do banco
    2. Junta o backup com a pasta data\uploads (fotos) num unico .zip
    3. Entra como admin no servidor no ar e envia o .zip
    4. Depois disso, voce REINICIA o servico no painel do Railway para aplicar

  Uso:
    .\enviar-para-railway.ps1 -UrlRailway https://seu-app.up.railway.app

  Atencao: substitui TODO o banco do servidor no ar (o banco anterior fica guardado como
  estoque.mv.db.bak-AAAAMMDD-HHMMSS no volume). Rode com o sistema local ligado.
  ==============================================================
#>

param(
    [Parameter(Mandatory = $true)][string]$UrlRailway,
    [string]$UrlLocal = "http://localhost:8080",
    [string]$PastaUploads = (Join-Path $PSScriptRoot "data\uploads")
)

$ErrorActionPreference = "Stop"
$UrlRailway = $UrlRailway.TrimEnd("/")
$UrlLocal   = $UrlLocal.TrimEnd("/")

function Ler-Senha($rotulo) {
    $seguro = Read-Host $rotulo -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function Entrar($url, $login, $senha) {
    $corpo = @{ login = $login; senha = $senha } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "$url/api/auth/login" -ContentType "application/json" -Body $corpo
    if (-not $r.admin) { throw "O login '$login' nao e administrador em $url." }
    return $r.token
}

if ($UrlRailway -notmatch "^https://") {
    throw "Use o endereco https:// do servidor no ar (senha e token nao devem trafegar sem HTTPS)."
}
if (-not (Test-Path $PastaUploads)) { throw "Pasta de fotos nao encontrada: $PastaUploads" }

$login = Read-Host "Login do admin"
$senhaLocal   = Ler-Senha "Senha do admin (sistema LOCAL)"
$senhaRailway = Ler-Senha "Senha do admin (servidor NO AR - a definida em APP_ADMIN_SENHA)"

$tmp = Join-Path ([IO.Path]::GetTempPath()) ("nexus-envio-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
    Write-Host "`n[1/4] Baixando backup do banco local..." -ForegroundColor Cyan
    $tokenLocal = Entrar $UrlLocal $login $senhaLocal
    $backupZip = Join-Path $tmp "backup.zip"
    Invoke-WebRequest -Uri "$UrlLocal/api/admin/empresas/backup" -Headers @{ Authorization = "Bearer $tokenLocal" } -OutFile $backupZip

    Write-Host "[2/4] Montando pacote (banco + fotos)..." -ForegroundColor Cyan
    $pacote = Join-Path $tmp "pacote"
    Expand-Archive -Path $backupZip -DestinationPath $pacote
    if (-not (Test-Path (Join-Path $pacote "estoque.mv.db"))) { throw "O backup nao trouxe estoque.mv.db." }
    Copy-Item -Path $PastaUploads -Destination (Join-Path $pacote "uploads") -Recurse
    $zipFinal = Join-Path $tmp "restaurar.zip"
    Compress-Archive -Path (Join-Path $pacote "*") -DestinationPath $zipFinal
    $mb = [math]::Round((Get-Item $zipFinal).Length / 1MB, 1)
    Write-Host "      Pacote com $mb MB"

    Write-Host "[3/4] Enviando para $UrlRailway ..." -ForegroundColor Cyan
    $tokenRailway = Entrar $UrlRailway $login $senhaRailway
    $resp = & curl.exe -sS --fail-with-body -X POST "$UrlRailway/api/admin/empresas/restaurar" `
        -H "Authorization: Bearer $tokenRailway" -F "arquivo=@$zipFinal;type=application/zip"
    if ($LASTEXITCODE -ne 0) { throw "Falha no envio: $resp" }
    Write-Host "      $resp"

    Write-Host "`n[4/4] Pronto. Agora REINICIE o servico no painel do Railway (Deployments > Restart)." -ForegroundColor Green
    Write-Host "      Depois de reiniciar, o site no ar passa a usar o mesmo banco, precos e fotos do local."
}
finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}
