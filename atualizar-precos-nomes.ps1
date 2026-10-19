<#
  ==============================================================
  atualizar-precos-nomes.ps1

  Atualiza preco e/ou nome dos modelos (categorias) da loja pela API do sistema.

  O que faz:
    1. Entra com o login da EMPRESA (o mesmo usado para cadastrar produtos)
    2. Mostra uma previa de tudo que vai mudar
    3. So grava depois que voce confirmar com "s"

  Uso:
    .\atualizar-precos-nomes.ps1                                 (servidor no ar: https://pafanny.com.br)
    .\atualizar-precos-nomes.ps1 -Url http://localhost:8080      (sistema local)

  Quando um modelo e renomeado, a categoria e o inicio do nome de cada produto mudam juntos
  (ex.: "Baby Doll Manga Stitch" -> "Baby Look Stitch"). Codigo, estampa, fotos e estoque nao mudam.
  ==============================================================
#>

param(
    [string]$Url = "https://pafanny.com.br"
)

$ErrorActionPreference = "Stop"
$Url = $Url.TrimEnd("/")
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# Regras: categoria atual -> novo nome (ou $null para manter) e novo preco de venda (ou $null para manter)
$regras = @(
    @{ De = "Camisola Babado";    Para = $null;                                  Preco = 68.99 },
    @{ De = "Camisola";           Para = $null;                                  Preco = 68.99 },
    @{ De = "Alcinha Vies Botao"; Para = $null;                                  Preco = 68.99 },
    @{ De = "Camiseta Botao";     Para = "Americano Curto com Shorts sem Gola";  Preco = 86.99 },
    @{ De = "Baby Doll Manga";    Para = "Baby Look";                            Preco = $null },
    @{ De = "Americano Curto";    Para = "Americano com Gola Curto (Shorts)";    Preco = 98.99 }
)

function Normalizar([string]$s) {
    if (-not $s) { return "" }
    $d = $s.Trim().ToLowerInvariant().Normalize([Text.NormalizationForm]::FormD)
    $sb = New-Object Text.StringBuilder
    foreach ($c in $d.ToCharArray()) {
        if ([Globalization.CharUnicodeInfo]::GetUnicodeCategory($c) -ne [Globalization.UnicodeCategory]::NonSpacingMark) { [void]$sb.Append($c) }
    }
    return ($sb.ToString() -replace '\s+', ' ')
}

# Le e envia JSON sempre em UTF-8 (o PowerShell 5.1 estragaria acentos de nomes e descricoes)
function Chamar($metodo, $caminho, $token, $corpo) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Method = $metodo; Uri = "$Url$caminho"; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $corpo) {
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = [Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Depth 5 -Compress))
    }
    $r = Invoke-WebRequest @params
    $ms = New-Object IO.MemoryStream
    $r.RawContentStream.Position = 0
    $r.RawContentStream.CopyTo($ms)
    $texto = [Text.Encoding]::UTF8.GetString($ms.ToArray())
    # Atribuir antes de retornar: no PowerShell 5.1 o ConvertFrom-Json entrega a lista como um item so
    if ($texto) { $obj = $texto | ConvertFrom-Json; return $obj }
}

function Ler-Senha($rotulo) {
    $seguro = Read-Host $rotulo -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

if ($Url -notmatch "^https://" -and $Url -notmatch "^http://(localhost|127\.0\.0\.1)(:\d+)?$") {
    throw "Use https:// (a senha nao deve trafegar sem HTTPS) ou o endereco local http://localhost:8080."
}

Write-Host "`nServidor: $Url" -ForegroundColor Cyan
$login = Read-Host "Login da empresa (ex.: pafanny)"
$senha = Ler-Senha "Senha"
$auth = Chamar "Post" "/api/auth/login" $null @{ login = $login; senha = $senha }
$senha = $null
if (-not $auth.token) { throw "Login falhou." }
if ($auth.admin) { throw "Esse e o login do ADMIN. Use o login da empresa (o que cadastra os produtos)." }
$token = $auth.token

$produtos = @(Chamar "Get" "/api/produtos" $token $null)
Write-Host "$($produtos.Count) produtos encontrados.`n"

$mudancas = @()
foreach ($regra in $regras) {
    $de = Normalizar $regra.De
    $alvo = @($produtos | Where-Object { (Normalizar $_.categoria) -eq $de })
    Write-Host ("== {0}: {1} produto(s)" -f $regra.De, $alvo.Count) -ForegroundColor Yellow
    foreach ($p in $alvo) {
        $novoNome = $p.nome
        $novaCategoria = $p.categoria
        if ($regra.Para) {
            $novaCategoria = $regra.Para
            if ((Normalizar $p.nome).StartsWith($de)) {
                $novoNome = $regra.Para + $p.nome.Substring($p.categoria.Trim().Length)
            }
        }
        $novoPreco = $p.precoVenda
        if ($null -ne $regra.Preco) { $novoPreco = $regra.Preco }

        $txtNome  = if ($novoNome -ne $p.nome) { "$($p.nome)  ->  $novoNome" } else { $p.nome }
        $txtPreco = if ($novoPreco -ne $p.precoVenda) { "R$ $($p.precoVenda) -> R$ $novoPreco" } else { "R$ $($p.precoVenda) (mantem)" }
        Write-Host "   #$($p.id)  $txtNome  |  $txtPreco"

        if ($novoNome -ne $p.nome -or $novaCategoria -ne $p.categoria -or $novoPreco -ne $p.precoVenda) {
            $mudancas += @{ P = $p; Nome = $novoNome; Categoria = $novaCategoria; Preco = $novoPreco }
        }
    }
    Write-Host ""
}

if ($mudancas.Count -eq 0) { Write-Host "Nada para mudar." -ForegroundColor Green; return }

$ok = Read-Host "Aplicar $($mudancas.Count) alteracao(oes) em $Url ? (s/n)"
if ($ok -ne "s") { Write-Host "Cancelado. Nada foi alterado."; return }

$falhas = 0
foreach ($m in $mudancas) {
    $p = $m.P
    # Mesmo formato que a tela de edicao envia: so os campos editaveis, o resto fica como esta
    $dados = @{
        nome            = $m.Nome
        quantidade      = $p.quantidade
        precoCusto      = $p.precoCusto
        precoVenda      = $m.Preco
        categoria       = $m.Categoria
        estampa         = $p.estampa
        codigo          = $p.codigo
        disponibilidade = $p.disponibilidade
        descricao       = $p.descricao
        visivelLoja     = $p.visivelLoja
    }
    try {
        [void](Chamar "Put" "/api/produtos/$($p.id)" $token $dados)
        Write-Host "   ok  #$($p.id) $($m.Nome)" -ForegroundColor Green
    } catch {
        $falhas++
        Write-Host "   ERRO #$($p.id) $($p.nome): $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
if ($falhas -eq 0) { Write-Host "Pronto: $($mudancas.Count) produto(s) atualizado(s)." -ForegroundColor Green }
else { Write-Host "$falhas falha(s). Rode de novo: o que ja foi alterado nao muda outra vez." -ForegroundColor Red }
