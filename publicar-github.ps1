<#
  ==============================================================
  GestaoNexus - publicar-github.ps1

  Automatiza git init/add/commit/push de forma segura:
    - Roda uma varredura simples por segredos antes de commitar
    - Nunca sobrescreve remoto sem confirmacao
    - Nunca faz force-push
    - Pede confirmacao explicita antes de cada acao irreversivel

  Uso:
    .\publicar-github.ps1
    .\publicar-github.ps1 -Mensagem "feat: ajustes na vitrine"
    .\publicar-github.ps1 -RepoUrl "https://github.com/usuario/repo.git"
  ==============================================================
#>

param(
    [string]$Mensagem = "",
    [string]$RepoUrl = "",
    [string]$Branch = "main"
)

# Nao usamos $ErrorActionPreference = "Stop": no Windows PowerShell 5.1,
# qualquer linha que o git escreva em stderr (mesmo avisos inofensivos,
# tipo "LF will be replaced by CRLF") vira um erro terminante e derruba
# o script. Cada comando git critico abaixo checa $LASTEXITCODE na mao.
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
Set-Location -Path $PSScriptRoot

function Write-Passo($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }
function Write-Ok($texto)    { Write-Host "  [OK] $texto" -ForegroundColor Green }
function Write-Erro($texto)  { Write-Host "  [ERRO] $texto" -ForegroundColor Red }
function Write-Aviso($texto) { Write-Host "  [AVISO] $texto" -ForegroundColor Yellow }

function Invoke-GitOuAborta {
    param([string[]]$GitArgs, [string]$MensagemErro)
    & git @GitArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Erro $MensagemErro
        exit 1
    }
}

# -- 1. Pre-requisitos -----------------------------------------
Write-Passo "Verificando Git"
try {
    git --version | Out-Null
    Write-Ok "Git encontrado"
} catch {
    Write-Erro "Git nao encontrado no PATH. Instale em https://git-scm.com/downloads e rode o script de novo."
    exit 1
}

$nomeConfigurado  = (git config user.name)
$emailConfigurado = (git config user.email)
if ([string]::IsNullOrWhiteSpace($nomeConfigurado) -or [string]::IsNullOrWhiteSpace($emailConfigurado)) {
    Write-Erro "Git ainda nao tem nome/e-mail configurados nesta maquina (necessario para o commit)."
    Write-Host "  Rode primeiro:"
    Write-Host '    git config --global user.name "Seu Nome"'
    Write-Host '    git config --global user.email "seu-email@exemplo.com"'
    exit 1
}
Write-Ok "Identidade do Git: $nomeConfigurado <$emailConfigurado>"

# -- 2. Inicializa repositorio se necessario --------------------
Write-Passo "Verificando repositorio Git"
if (-not (Test-Path ".git")) {
    Invoke-GitOuAborta -GitArgs @("init") -MensagemErro "Falha ao rodar 'git init'."
    Write-Ok "Repositorio Git inicializado em $PSScriptRoot"
} else {
    Write-Ok "Repositorio Git ja existe"
}

if (-not (Test-Path ".gitignore")) {
    Write-Erro ".gitignore nao encontrado - abortando por seguranca (sem ele, arquivos sensiveis podem ser versionados)."
    exit 1
}

# -- 3. Varredura simples por segredos ---------------------------
Write-Passo "Procurando possiveis segredos nos arquivos que serao versionados"

$arquivosParaChecar = git ls-files --others --exclude-standard --cached

# Arquivos que legitimamente citam "senha"/"secret" em prosa ou como
# placeholder de exemplo -- nao sao escaneados.
$arquivosIgnorados = @(
    "publicar-github.ps1",
    "README.md",
    "LICENSE"
)

# Padroes genericos, validos em qualquer tipo de arquivo.
$padroesGlobais = @(
    'BEGIN PRIVATE KEY',
    'BEGIN RSA PRIVATE KEY',
    '\bAKIA[0-9A-Z]{16}\b'
)

# application.properties / .env / .yml: uma chave de segredo com valor
# literal (nao um placeholder ${VAR} ou ${VAR:} puro) e um segredo hardcoded.
$extensoesConfig = @(".properties", ".env", ".yml", ".yaml")
$padraoConfig = '(?im)^\s*[\w.\-]*\b(secret|senha|password|token|apikey|api[_-]?key)\b\s*=\s*(?!\$\{[A-Z0-9_]+:?\}\s*$).+$'

# Codigo-fonte: atribuicao de uma string literal a uma variavel de senha/segredo.
$extensoesCodigo = @(".java", ".js", ".ts", ".html", ".py")
$padraoCodigo = '(?i)\b(password|senha|secret|apikey)\b\s*=\s*"[^"$][^"]{2,}"'

$encontrouSegredo = $false
foreach ($arquivo in $arquivosParaChecar) {
    if (-not (Test-Path $arquivo)) { continue }
    if ($arquivo -like "*.example") { continue }
    if ($arquivo -like "docs/screenshots/*") { continue }
    if ($arquivosIgnorados -contains (Split-Path $arquivo -Leaf)) { continue }

    $conteudo = Get-Content $arquivo -Raw -ErrorAction SilentlyContinue
    if (-not $conteudo) { continue }

    $extensao = [System.IO.Path]::GetExtension($arquivo)
    $padroes = [System.Collections.Generic.List[string]]::new()
    $padroes.AddRange([string[]]$padroesGlobais)
    if ($extensoesConfig -contains $extensao) { $padroes.Add($padraoConfig) }
    if ($extensoesCodigo -contains $extensao) { $padroes.Add($padraoCodigo) }

    foreach ($padrao in $padroes) {
        if ($conteudo -match $padrao) {
            Write-Aviso "Possivel segredo em '$arquivo'"
            $encontrouSegredo = $true
        }
    }
}

if ($encontrouSegredo) {
    Write-Erro "Segredos em potencial encontrados. Revise os arquivos acima antes de continuar."
    $continuar = Read-Host "Digite CONTINUAR para prosseguir mesmo assim, ou qualquer outra tecla para abortar"
    if ($continuar -ne "CONTINUAR") {
        Write-Host "Abortado pelo usuario." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Ok "Nenhum padrao suspeito encontrado"
}

# -- 4. Status atual ----------------------------------------------
Write-Passo "Status do repositorio"
git status --short

# -- 5. Stage + commit ---------------------------------------------
Write-Passo "Adicionando arquivos ao commit"
Invoke-GitOuAborta -GitArgs @("add", "-A") -MensagemErro "Falha ao rodar 'git add -A'."
git status --short

$arquivosStaged = git diff --cached --name-only
if (-not $arquivosStaged) {
    Write-Aviso "Nao ha mudancas para commitar."
} else {
    if ([string]::IsNullOrWhiteSpace($Mensagem)) {
        $Mensagem = Read-Host "Mensagem do commit (Enter para usar a padrao)"
        if ([string]::IsNullOrWhiteSpace($Mensagem)) {
            $Mensagem = "chore: atualiza GestaoNexus"
        }
    }
    Invoke-GitOuAborta -GitArgs @("commit", "-m", $Mensagem) -MensagemErro "Falha ao criar o commit - veja a mensagem do Git acima."
    Write-Ok "Commit criado: `"$Mensagem`""
}

# -- 6. Configura branch principal ------------------------------
Invoke-GitOuAborta -GitArgs @("branch", "-M", $Branch) -MensagemErro "Falha ao renomear a branch para '$Branch'."

# -- 7. Configura remoto -----------------------------------------
Write-Passo "Verificando remoto 'origin'"
$origemExiste = git remote get-url origin 2>$null
if ($LASTEXITCODE -ne 0 -or -not $origemExiste) {
    if ([string]::IsNullOrWhiteSpace($RepoUrl)) {
        $RepoUrl = Read-Host "Cole a URL do repositorio no GitHub (ex.: https://github.com/usuario/repo.git)"
    }
    if ([string]::IsNullOrWhiteSpace($RepoUrl)) {
        Write-Erro "Nenhuma URL de repositorio informada. Abortando antes do push."
        exit 1
    }
    Invoke-GitOuAborta -GitArgs @("remote", "add", "origin", $RepoUrl) -MensagemErro "Falha ao configurar o remoto 'origin'."
    Write-Ok "Remoto 'origin' configurado: $RepoUrl"
} else {
    Write-Ok "Remoto 'origin' ja configurado: $origemExiste"
}

# -- 8. Confirmacao final antes do push ---------------------------
Write-Passo "Pronto para enviar ao GitHub"
$destino = git remote get-url origin
Write-Host "  Branch : $Branch"
Write-Host "  Destino: $destino"
$confirmacao = Read-Host "`nConfirmar push? (s/N)"
if ($confirmacao -ne "s" -and $confirmacao -ne "S") {
    Write-Host "Push cancelado pelo usuario." -ForegroundColor Yellow
    exit 0
}

# -- 9. Push (nunca --force) --------------------------------------
Write-Passo "Enviando para o GitHub"
Invoke-GitOuAborta -GitArgs @("push", "-u", "origin", $Branch) -MensagemErro "Falha ao enviar para o GitHub - veja a mensagem do Git acima (confira usuario/permissoes/URL do repositorio)."
Write-Ok "Publicado com sucesso!"
