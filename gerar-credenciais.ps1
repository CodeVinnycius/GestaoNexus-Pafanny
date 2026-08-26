<#
  Gera as credenciais de producao (JWT secret + senha do admin) na
  primeira vez que o sistema roda nesta maquina, e salva num arquivo
  local (.env.producao.bat) que NUNCA vai para o Git.
#>

$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$jwtSecret = [Convert]::ToBase64String($bytes)

$chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'
$senha = -join (1..20 | ForEach-Object { $chars[(Get-Random -Maximum $chars.Length)] })

# Escrito como array de linhas (nao here-string) para garantir quebra
# de linha CRLF de verdade -- cmd.exe recusa .bat com LF puro.
$linhas = @(
    "@echo off",
    "set `"APP_JWT_SECRET=$jwtSecret`"",
    "set `"APP_ADMIN_SENHA=$senha`"",
    "set `"APP_LOJA_WHATSAPP=5511986032979`"",
    "set `"APP_LOJA_INSTAGRAM=usepafanny`""
)

Set-Content -Path ".env.producao.bat" -Value $linhas -Encoding ASCII

Write-Host ""
Write-Host " ============================================"
Write-Host "  Configuracao de seguranca criada!"
Write-Host " ============================================"
Write-Host ""
Write-Host " Sua senha de administrador e:"
Write-Host ""
Write-Host "     $senha" -ForegroundColor Green
Write-Host ""
Write-Host " ANOTE essa senha em lugar seguro agora."
Write-Host " Ela serve so para entrar como ADMINISTRADOR do sistema"
Write-Host " (nao e a senha da loja para os clientes)."
Write-Host ""
