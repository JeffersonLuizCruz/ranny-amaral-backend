package com.ranny.fotografia.arquivo.service;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.shared.exception.ArquivoInvalidoException;
import com.ranny.fotografia.shared.exception.ArquivoMuitoGrandeException;
import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida metadados na abertura do upload e o conteudo real na conclusao.
 *
 * <p>PATTERN: Specification / Guard Clauses.
 */
@Component
public class ValidadorArquivo {

    private static final Pattern CHECKSUM = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final int NOME_MAXIMO = 255;

    public ArquivoValidado validarInicio(ArquivoFinalidade finalidade, String nomeOriginal,
                                         String mimeDeclarado, long tamanhoBytes,
                                         String checksum, long tamanhoMaximo) {
        if (tamanhoBytes <= 0) {
            throw new RequisicaoInvalidaException("ARQUIVO_VAZIO",
                    "O arquivo enviado esta vazio.");
        }
        if (tamanhoBytes > tamanhoMaximo) {
            throw new ArquivoMuitoGrandeException(
                    "O arquivo deve ter no maximo %d MB.".formatted(tamanhoMaximo / (1024 * 1024)));
        }
        if (checksum == null || !CHECKSUM.matcher(checksum).matches()) {
            throw new ArquivoInvalidoException("CHECKSUM_INVALIDO",
                    "Informe o checksum SHA-256 do arquivo.");
        }
        TipoArquivo tipo = TipoArquivo.porMime(mimeDeclarado);
        Set<TipoArquivo> permitidos = TipoArquivo.permitidos(finalidade);
        if (tipo == null || !permitidos.contains(tipo)) {
            throw new ArquivoInvalidoException("ARQUIVO_TIPO_INVALIDO",
                    mensagemTiposPermitidos(finalidade));
        }
        return new ArquivoValidado(
                sanitizarNome(nomeOriginal, tipo.extensao()),
                tipo.mimeType(),
                tipo.extensao());
    }

    private static String mensagemTiposPermitidos(ArquivoFinalidade finalidade) {
        return switch (finalidade) {
            case COMPROVANTE_PAGAMENTO -> "Envie um comprovante PDF, JPEG, PNG ou WebP.";
            case CONTRATO_PDF -> "Envie um contrato em PDF.";
            case FOTO_ORIGINAL, FOTO_DERIVADO, FOTO_EDITADA ->
                    "Envie uma foto JPEG, PNG ou WebP.";
            case VIDEO_ORIGINAL -> "Envie um video MP4 ou MOV.";
        };
    }

    public TipoArquivo validarConteudo(byte[] primeirosBytes) {
        TipoArquivo detectado = TipoArquivo.detectar(primeirosBytes);
        if (detectado == null) {
            throw new ArquivoInvalidoException("ARQUIVO_TIPO_INVALIDO",
                    "O conteudo do arquivo nao corresponde a um PDF, JPEG, PNG ou WebP.");
        }
        return detectado;
    }

    private static String sanitizarNome(String nome, String extensaoPadrao) {
        String valor = nome == null ? "" : nome.replace('\\', '/');
        int barra = valor.lastIndexOf('/');
        if (barra >= 0) {
            valor = valor.substring(barra + 1);
        }
        valor = valor.replaceAll("\\p{Cntrl}", "").trim();
        if (valor.isBlank()) {
            valor = "arquivo." + extensaoPadrao;
        }
        if (valor.length() > NOME_MAXIMO) {
            valor = valor.substring(0, NOME_MAXIMO);
        }
        return valor.toLowerCase(Locale.ROOT).endsWith("." + extensaoPadrao)
                ? valor
                : valor + "." + extensaoPadrao;    }

    public record ArquivoValidado(String nomeOriginal, String mimeType, String extensao) {
    }
}
