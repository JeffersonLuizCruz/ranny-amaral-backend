package com.ranny.fotografia.arquivo.service

import com.ranny.fotografia.arquivo.ArquivoFinalidade
import com.ranny.fotografia.arquivo.ArquivoUpload
import com.ranny.fotografia.arquivo.storage.FakeObjectStorage
import com.ranny.fotografia.shared.exception.ArquivoInvalidoException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArquivoServiceSpec extends Specification {

    @Autowired
    ArquivoUpload arquivoUpload

    @Autowired
    FakeObjectStorage storage

    def "inicia e conclui multipart de foto"() {
        given:
        def conteudo = imagem(9 * 1024 * 1024)

        when:
        def sessao = arquivoUpload.iniciar(new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.FOTO_ORIGINAL,
                "galeria-1",
                "testes/galerias/galeria-1",
                "foto.jpg",
                "image/jpeg",
                conteudo.length,
                "a" * 64,
                50L * 1024 * 1024))

        then:
        sessao.estrategia() == ArquivoUpload.EstrategiaUpload.MULTIPART
        sessao.partes().size() == 2

        when:
        storage.simular(conteudo.length, "image/jpeg", conteudo)
        def resumo = arquivoUpload.concluir(
                sessao.arquivoId(), ArquivoFinalidade.FOTO_ORIGINAL, "galeria-1",
                [new ArquivoUpload.ParteEnviada(1, "etag-1"),
                 new ArquivoUpload.ParteEnviada(2, "etag-2")])

        then:
        resumo.status() == com.ranny.fotografia.arquivo.ArquivoStatus.DISPONIVEL
        resumo.tamanhoBytes() == conteudo.length
        resumo.objectKey().contains(sessao.arquivoId())
    }

    def "deduplica pelo checksum e referencia"() {
        given:
        def conteudo = imagem(1024)
        def checksum = "b" * 64
        storage.simular(conteudo.length, "image/jpeg", conteudo)
        def primeira = arquivoUpload.iniciar(new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.FOTO_ORIGINAL, "galeria-2", "testes/galerias/galeria-2",
                "a.jpg", "image/jpeg", conteudo.length, checksum, 50L * 1024 * 1024))
        arquivoUpload.concluir(primeira.arquivoId(), ArquivoFinalidade.FOTO_ORIGINAL,
                "galeria-2", [])

        when:
        def segunda = arquivoUpload.iniciar(new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.FOTO_ORIGINAL, "galeria-2", "testes/galerias/galeria-2",
                "b.jpg", "image/jpeg", conteudo.length, checksum, 50L * 1024 * 1024))

        then:
        segunda.reutilizado()
        segunda.estrategia() == ArquivoUpload.EstrategiaUpload.REUTILIZADO
        segunda.arquivoId() == primeira.arquivoId()
    }

    def "rejeita conteudo que nao corresponde ao tipo"() {
        given:
        def sessao = arquivoUpload.iniciar(new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.COMPROVANTE_PAGAMENTO, "contrato-1", "testes/contratos",
                "comprovante.pdf", "application/pdf", 2048, "c" * 64, 10L * 1024 * 1024))
        storage.simular(2048, "application/pdf", "<html>malicioso</html>".bytes)

        when:
        arquivoUpload.concluir(sessao.arquivoId(), ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                "contrato-1", [])

        then:
        thrown(ArquivoInvalidoException)
    }

    private static byte[] imagem(int tamanho) {
        byte[] bytes = new byte[tamanho]
        byte[] jpeg = [(byte) 0xFF, (byte) 0xD8, (byte) 0xFF]
        System.arraycopy(jpeg, 0, bytes, 0, jpeg.length)
        Arrays.fill(bytes, jpeg.length, bytes.length, (byte) 'x')
        return bytes
    }
}
