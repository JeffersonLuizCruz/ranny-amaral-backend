package com.ranny.fotografia.arquivo.service

import com.ranny.fotografia.arquivo.ArquivoFinalidade
import com.ranny.fotografia.shared.exception.ArquivoInvalidoException
import com.ranny.fotografia.shared.exception.ArquivoMuitoGrandeException
import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException
import spock.lang.Specification
import spock.lang.Unroll

class ValidadorArquivoSpec extends Specification {

    private final ValidadorArquivo validador = new ValidadorArquivo()

    @Unroll
    def "aceita #mime para #finalidade"() {
        when:
        def validado = validador.validarInicio(finalidade, "arquivo", mime, 1024, "a" * 64, 10 * 1024 * 1024)

        then:
        validado.mimeType() == mime
        validado.nomeOriginal().startsWith("arquivo.")

        where:
        finalidade                           | mime
        ArquivoFinalidade.COMPROVANTE_PAGAMENTO | "application/pdf"
        ArquivoFinalidade.COMPROVANTE_PAGAMENTO | "image/jpeg"
        ArquivoFinalidade.COMPROVANTE_PAGAMENTO | "image/png"
        ArquivoFinalidade.COMPROVANTE_PAGAMENTO | "image/webp"
        ArquivoFinalidade.FOTO_ORIGINAL      | "image/jpeg"
        ArquivoFinalidade.VIDEO_ORIGINAL     | "video/mp4"
        ArquivoFinalidade.VIDEO_ORIGINAL     | "video/quicktime"
    }

    @Unroll
    def "rejeita tipo #mime para #finalidade"() {
        when:
        validador.validarInicio(finalidade, "arquivo", mime, 1024, "a" * 64, 10 * 1024 * 1024)

        then:
        def erro = thrown(ArquivoInvalidoException)
        erro.codigo == "ARQUIVO_TIPO_INVALIDO"

        where:
        finalidade                           | mime
        ArquivoFinalidade.COMPROVANTE_PAGAMENTO | "video/mp4"
        ArquivoFinalidade.FOTO_ORIGINAL      | "application/pdf"
        ArquivoFinalidade.VIDEO_ORIGINAL     | "image/jpeg"
    }

    def "rejeita arquivo vazio e acima do limite"() {
        when:
        validador.validarInicio(ArquivoFinalidade.COMPROVANTE_PAGAMENTO, "x.pdf",
                "application/pdf", 0, "a" * 64, 100)

        then:
        thrown(RequisicaoInvalidaException)

        when:
        validador.validarInicio(ArquivoFinalidade.COMPROVANTE_PAGAMENTO, "x.pdf",
                "application/pdf", 101, "a" * 64, 100)

        then:
        thrown(ArquivoMuitoGrandeException)
    }

    def "rejeita checksum invalido"() {
        when:
        validador.validarInicio(ArquivoFinalidade.COMPROVANTE_PAGAMENTO, "x.pdf",
                "application/pdf", 100, "xyz", 100)

        then:
        thrown(ArquivoInvalidoException)
    }

    def "detecta magic bytes de imagem e video"() {
        expect:
        validador.validarConteudo("%PDF-1.7".bytes).name() == "PDF"
        validador.validarConteudo([(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0] as byte[]).name() == "JPEG"
        validador.validarConteudo([(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] as byte[]).name() == "PNG"
        validador.validarConteudo("RIFF0000WEBP".bytes).name() == "WEBP"
        validador.validarConteudo("0000ftypisom".bytes).name() == "MP4"
    }

    def "rejeita conteudo desconhecido"() {
        when:
        validador.validarConteudo("<html>malicioso</html>".bytes)

        then:
        thrown(ArquivoInvalidoException)
    }

    def "sanitiza nome com caminho e caracteres de controle"() {
        when:
        def validado = validador.validarInicio(ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                "../pasta/meu\narquivo", "application/pdf", 100, "a" * 64, 100)

        then:
        validado.nomeOriginal() == "meuarquivo.pdf"
    }
}
