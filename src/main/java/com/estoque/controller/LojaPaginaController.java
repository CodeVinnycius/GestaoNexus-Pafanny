package com.estoque.controller;

import com.estoque.dto.ProdutoPublicoDTO;
import com.estoque.service.LojaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Página da loja (/ e /loja.html) com as peças principais já em HTML, para quem está sem JavaScript.
 * Com JS nada muda: as peças vão num &lt;noscript&gt; logo após o grid e o loja.js continua montando o catálogo.
 * Qualquer falha devolve o loja.html original — a página inicial nunca quebra por causa disto.
 */
@Controller
public class LojaPaginaController {

    private static final Logger log = LoggerFactory.getLogger(LojaPaginaController.class);

    private static final int PECAS_SEM_JS = 8;

    /** Mesma ordem do ORDEM_CATEGORIAS do js/loja.js. */
    private static final List<String> ORDEM_CATEGORIAS = List.of(
        "Alcinha Babado", "Alcinha Botão", "Alcinha Vies Botao", "Alcinha Tapeta",
        "Camiseta Botão", "Camisola",
        "Baby Doll Manga", "Baby Doll Infantil",
        "Longo Americano", "Longo Americano de Inverno",
        "Pijama Longo Botao", "Americano Curto");

    private static final Map<String, String[]> ROTULO_DISPONIBILIDADE = Map.of(
        "DISPONIVEL", new String[]{"Disponível", "disponivel"},
        "ULTIMAS_UNIDADES", new String[]{"Últimas unidades", "ultimas"},
        "INDISPONIVEL", new String[]{"Indisponível", "indisponivel"},
        "ESGOTADO", new String[]{"Esgotado", "esgotado"});

    private static final String MARCADOR_GRID = "<p class=\"loja-msg\">Carregando peças…</p>";
    private static final List<String> IDS_WHATSAPP = List.of(
        "cta-header-whatsapp", "cta-mobile-whatsapp", "cta-hero-whatsapp", "fab-whatsapp", "footer-whatsapp");
    private static final List<String> IDS_INSTAGRAM = List.of("link-instagram", "footer-instagram");

    /** Ícone do WhatsApp definido uma vez no <noscript> e reaproveitado em cada card (HTML menor). */
    private static final String SIMBOLO_WHATSAPP = "<svg width=\"0\" height=\"0\" style=\"position:absolute\" aria-hidden=\"true\"><symbol id=\"icone-whatsapp\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12.04 2c-5.5 0-9.96 4.46-9.96 9.96 0 1.76.46 3.45 1.32 4.95L2 22l5.25-1.38a9.9 9.9 0 0 0 4.79 1.22h.01c5.5 0 9.96-4.46 9.96-9.96S17.54 2 12.04 2Zm5.86 14.06c-.25.7-1.24 1.29-2.02 1.45-.55.11-1.27.2-3.69-.79-3.1-1.28-5.1-4.41-5.25-4.61-.15-.2-1.26-1.67-1.26-3.19 0-1.52.79-2.26 1.08-2.57.24-.26.55-.35.78-.35.15 0 .3.01.43.02.34.02.51.04.72.5.25.55.85 1.98.92 2.13.07.15.12.32.02.52-.09.2-.14.32-.28.5-.14.17-.29.38-.42.5-.14.13-.29.28-.13.55.16.27.71 1.17 1.53 1.9 1.05.94 1.94 1.23 2.21 1.37.27.13.42.11.58-.07.16-.19.68-.79.87-1.06.18-.27.36-.22.6-.13.25.09 1.58.74 1.85.88.27.13.45.2.52.31.06.12.06.68-.19 1.38Z\"/></symbol></svg>";
    private static final String ICONE_WHATSAPP = "<svg aria-hidden=\"true\"><use href=\"#icone-whatsapp\"/></svg>";

    private final LojaService service;
    private final String modelo;

    public LojaPaginaController(LojaService service) throws IOException {
        this.service = service;
        this.modelo = new ClassPathResource("static/loja.html").getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping(value = {"/", "/loja.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String pagina() {
        try {
            return montar();
        } catch (RuntimeException e) {
            log.warn("Loja servida sem o catálogo em HTML: {}", e.toString());
            return modelo;
        }
    }

    private String montar() {
        Map<String, String> config = service.config();
        String nome = vazioOu(config.get("nome"), "PAFANNY");
        String numero = vazioOu(config.get("whatsapp"), "").replaceAll("\\D", "");
        String instagram = "https://instagram.com/" + vazioOu(config.get("instagram"), "usepafanny");

        String html = modelo;
        String generica = linkWhatsapp(numero, "Olá! 💛 Gostaria de saber mais sobre os pijamas da " + nome + ".");
        for (String id : IDS_WHATSAPP) html = trocarHref(html, id, generica);
        html = trocarHref(html, "cta-tamanhos-whatsapp",
            linkWhatsapp(numero, "Olá! 💛 Gostaria de ajuda para escolher o tamanho ideal de um pijama " + nome + "."));
        for (String id : IDS_INSTAGRAM) html = trocarHref(html, id, instagram);

        List<ProdutoPublicoDTO> produtos = new ArrayList<>(service.listarVisiveis());
        produtos.sort(Comparator.comparingInt(p -> pesoCategoria(p.getCategoria())));
        // O <noscript> fica DEPOIS do #grid-produtos, com um grid próprio dentro: com JS o navegador
        // nunca o desenha (dentro do grid, um display:contents fazia o HTML cru aparecer como texto).
        StringBuilder cards = new StringBuilder("<noscript><div class=\"grid-produtos\">").append(SIMBOLO_WHATSAPP);
        produtos.stream().limit(PECAS_SEM_JS).forEach(p -> cards.append(card(p, nome, numero)));
        cards.append("</div></noscript>");

        int marcador = html.indexOf(MARCADOR_GRID);
        int fimDoGrid = marcador < 0 ? -1 : html.indexOf("</div>", marcador);
        if (fimDoGrid < 0) return html;
        int depois = fimDoGrid + "</div>".length();
        return html.substring(0, depois) + cards + html.substring(depois);
    }

    private String card(ProdutoPublicoDTO p, String nomeLoja, String numero) {
        String nome = esc(p.getNome());
        StringBuilder s = new StringBuilder("<article class=\"produto-card\"><div class=\"produto-card__img\">");
        if (p.getImagens() != null && !p.getImagens().isEmpty()) {
            s.append("<img src=\"").append(esc(p.getImagens().get(0))).append("\" alt=\"Pijama ").append(nome)
             .append("\" loading=\"lazy\" decoding=\"async\"/>");
        } else {
            s.append("<span class=\"sem-foto\" aria-hidden=\"true\">♡</span>");
        }
        String[] badge = p.getDisponibilidade() == null ? null : ROTULO_DISPONIBILIDADE.get(p.getDisponibilidade().name());
        if (badge != null) {
            s.append("<span class=\"produto-card__badge ").append(badge[1]).append("\">").append(badge[0]).append("</span>");
        }
        s.append("<a class=\"produto-card__wpp\" href=\"").append(esc(linkWhatsapp(numero, mensagemProduto(p, nomeLoja))))
         .append("\" target=\"_blank\" rel=\"noopener\" aria-label=\"Pedir ").append(nome).append(" pelo WhatsApp\">")
         .append(ICONE_WHATSAPP).append("</a></div><div class=\"produto-card__info\">");
        if (p.getCategoria() != null && !p.getCategoria().isBlank()) {
            s.append("<div class=\"produto-card__categoria\">").append(esc(p.getCategoria())).append("</div>");
        }
        s.append("<h3 class=\"produto-card__nome\">").append(nome).append("</h3>")
         .append("<div class=\"produto-card__preco").append(p.getPreco() == null ? " consultar" : "").append("\">")
         .append(p.getPreco() != null ? moeda(p.getPreco()) : "Consulte o valor").append("</div></div></article>");
        return s.toString();
    }

    /** Mesmo texto do mensagemProduto() do js/loja.js. */
    private static String mensagemProduto(ProdutoPublicoDTO p, String nomeLoja) {
        List<String> linhas = new ArrayList<>(List.of(
            "Olá! 💛",
            "Vi este pijama no site da " + nomeLoja + " e gostaria de saber se ele está disponível.",
            "",
            "Produto: " + p.getNome()));
        if (temTexto(p.getCategoria())) linhas.add("Categoria: " + p.getCategoria());
        if (temTexto(p.getEstampa()))   linhas.add("Estampa: " + p.getEstampa());
        linhas.add("Preço: " + (p.getPreco() != null ? moeda(p.getPreco()) : "Consulte o valor"));
        if (temTexto(p.getCodigo()))    linhas.add("Código: " + p.getCodigo());
        linhas.add("");
        linhas.add("Quais tamanhos estão disponíveis?");
        return String.join("\n", linhas);
    }

    private static String trocarHref(String html, String id, String url) {
        Matcher m = Pattern.compile("<a\\b[^>]*\\bid=\"" + Pattern.quote(id) + "\"[^>]*>").matcher(html);
        if (!m.find()) return html;
        String tag = m.group().replace("href=\"#\"", "href=\"" + esc(url) + "\"");
        return html.substring(0, m.start()) + tag + html.substring(m.end());
    }

    private static int pesoCategoria(String categoria) {
        int i = ORDEM_CATEGORIAS.indexOf(categoria);
        return i == -1 ? 999 : i;
    }

    private static String linkWhatsapp(String numero, String texto) {
        return "https://wa.me/" + numero + "?text=" + encodeURIComponent(texto);
    }

    /** Equivalente ao encodeURIComponent do JavaScript (o URLEncoder do Java troca espaço por "+"). */
    private static String encodeURIComponent(String texto) {
        StringBuilder s = new StringBuilder();
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || "-_.!~*'()".indexOf(c) >= 0) {
                s.append(c);
            } else {
                s.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return s.toString();
    }

    private static String moeda(double valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor);
    }

    private static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8"); }
    private static boolean temTexto(String s) { return s != null && !s.isEmpty(); }
    private static String vazioOu(String s, String padrao) { return s == null || s.isBlank() ? padrao : s; }
}
