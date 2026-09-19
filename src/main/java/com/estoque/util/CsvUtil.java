package com.estoque.util;

import java.util.Locale;

/** Helpers de formatação para exportações CSV (delimitador ';', padrão Excel pt-BR). */
public final class CsvUtil {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private CsvUtil() {}

    /** Escapa um campo de texto, citando quando contém delimitador, aspas ou quebra de linha. */
    public static String campo(String valor) {
        if (valor == null) return "";
        String v = valor.replace("\"", "\"\"");
        if (iniciaComGatilhoDeFormula(v)) v = "'" + v;
        return (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r"))
            ? "\"" + v + "\"" : v;
    }

    /**
     * Detecta prefixos que Excel/LibreOffice/Sheets interpretam como início de fórmula
     * (=, +, -, @, tab) — sem isso, texto de produto/movimentação poderia virar
     * "CSV/Formula Injection" ao ser aberto pela empresa numa planilha.
     */
    private static boolean iniciaComGatilhoDeFormula(String v) {
        if (v.isEmpty()) return false;
        char c = v.charAt(0);
        return c == '=' || c == '+' || c == '-' || c == '@' || c == '\t';
    }

    /** Formata número com vírgula decimal (padrão Excel pt-BR). */
    public static String numero(double v) {
        return String.format(PT_BR, "%.2f", v);
    }

    /** Monta o corpo HTTP com BOM UTF-8 (garante acentuação correta ao abrir no Excel). */
    public static byte[] paraBytesComBom(String csv) {
        byte[] conteudo = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] corpo = new byte[bom.length + conteudo.length];
        System.arraycopy(bom, 0, corpo, 0, bom.length);
        System.arraycopy(conteudo, 0, corpo, bom.length, conteudo.length);
        return corpo;
    }
}
