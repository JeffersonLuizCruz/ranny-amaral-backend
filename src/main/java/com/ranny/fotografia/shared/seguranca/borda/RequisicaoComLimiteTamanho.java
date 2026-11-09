package com.ranny.fotografia.shared.seguranca.borda;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Decora a requisicao para impor um teto de bytes no corpo.
 *
 * <p>PATTERN: Decorator.
 *
 * <p><b>Motivo:</b> o {@code Content-Length} pode estar ausente (chunked) ou
 * mentir. Sem um limite no stream, um cliente malicioso pode enviar um corpo
 * arbitrariamente grande e esgotar memoria/CPU do parser.
 *
 * <p><b>Beneficio:</b> controllers e converters continuam recebendo um
 * {@code HttpServletRequest} normal, sem saber que o stream e limitado. A
 * excecao {@link LimitePayloadExcedidoException} e lancada no momento em que o
 * limite e ultrapassado, antes de o payload inteiro ser materializado.
 */
public class RequisicaoComLimiteTamanho extends HttpServletRequestWrapper {

    private final long maxBytes;
    private long bytesLidos;
    private ServletInputStream inputStream;
    private BufferedReader reader;

    public RequisicaoComLimiteTamanho(HttpServletRequest request, long maxBytes) {
        super(request);
        this.maxBytes = maxBytes > 0 ? maxBytes : Long.MAX_VALUE;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (reader != null) {
            throw new IllegalStateException("getReader() ja foi chamado nesta requisicao.");
        }
        if (inputStream == null) {
            inputStream = new InputStreamLimitado(super.getInputStream());
        }
        return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            if (inputStream != null) {
                throw new IllegalStateException("getInputStream() ja foi chamado nesta requisicao.");
            }
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            inputStream = new InputStreamLimitado(super.getInputStream());
            reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        }
        return reader;
    }

    private void contabilizar(int quantidade) {
        if (quantidade <= 0) {
            return;
        }
        bytesLidos += quantidade;
        if (bytesLidos > maxBytes) {
            throw new LimitePayloadExcedidoException(maxBytes);
        }
    }

    /**
     * Stream que conta bytes e interrompe a leitura assim que o limite e
     * excedido. Le um byte a mais que o restante para detectar overflow sem
     * entregar o byte ao chamador.
     */
    private final class InputStreamLimitado extends ServletInputStream {

        private final ServletInputStream delegate;

        private InputStreamLimitado(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int valor = delegate.read();
            if (valor != -1) {
                contabilizar(1);
            }
            return valor;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            long restante = maxBytes - bytesLidos;
            int permitido = restante >= length
                    ? length
                    : (int) Math.min(length, restante + 1);
            int lidos = delegate.read(buffer, offset, permitido);
            if (lidos > 0) {
                contabilizar(lidos);
            }
            return lidos;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
