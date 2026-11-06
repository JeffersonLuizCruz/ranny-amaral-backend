package com.ranny.fotografia.galeria.service;

import com.ranny.fotografia.shared.exception.ArquivoInvalidoException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Implementacao Java de derivados de foto.
 *
 * <p>Gera thumb (400 px) e web (1600 px) em JPEG, preservando proporcao e
 * aplicando fundo branco quando a origem tem transparencia.
 */
@Component
public class ThumbnailatorProcessadorMidia implements ProcessadorMidia {

    private static final int THUMB = 400;
    private static final int WEB = 1600;

    @Override
    public Resultado processar(byte[] conteudo, String nomeOriginal) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(conteudo));
            if (original == null) {
                throw new ArquivoInvalidoException("IMAGEM_INVALIDA",
                        "Nao foi possivel decodificar a imagem enviada.");
            }

            Derivado thumb = gerar(original, THUMB, "thumb-400.jpg", "THUMB");
            Derivado web = gerar(original, WEB, "web-1600.jpg", "WEB");
            return new Resultado(original.getWidth(), original.getHeight(), List.of(thumb, web));
        } catch (IOException ex) {
            throw new ArquivoInvalidoException("IMAGEM_INVALIDA",
                    "Falha ao processar a imagem: " + ex.getMessage());
        }
    }

    private static Derivado gerar(BufferedImage original, int limite, String nome,
                                   String tipo) throws IOException {
        BufferedImage redimensionada = Thumbnails.of(original)
                .size(limite, limite)
                .asBufferedImage();
        BufferedImage rgb = new BufferedImage(
                redimensionada.getWidth(), redimensionada.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(redimensionada, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", saida);
        return new Derivado(tipo, nome, "image/jpeg", saida.toByteArray(),
                rgb.getWidth(), rgb.getHeight());
    }
}
