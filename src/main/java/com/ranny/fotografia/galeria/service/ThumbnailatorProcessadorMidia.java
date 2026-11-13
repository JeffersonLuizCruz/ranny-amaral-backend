package com.ranny.fotografia.galeria.service;

import com.ranny.fotografia.shared.exception.ArquivoInvalidoException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Implementacao Java de derivados de foto.
 *
 * <p>Gera thumb (400 px) e web (1600 px) em JPEG, preservando proporcao e
 * aplicando fundo branco quando a origem tem transparencia. Para cada tamanho
 * tambem gera uma versao com marca d'agua do estudio; o arquivo limpo continua
 * sendo o unico usado no download.
 */
@Component
public class ThumbnailatorProcessadorMidia implements ProcessadorMidia {

    private static final int THUMB = 400;
    private static final int WEB = 1600;
    private static final String MARCA_PADRAO = "Estudio";

    @Override
    public Resultado processar(byte[] conteudo, String nomeOriginal, String marcaDagua) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(conteudo));
            if (original == null) {
                throw new ArquivoInvalidoException("IMAGEM_INVALIDA",
                        "Nao foi possivel decodificar a imagem enviada.");
            }

            Derivado thumb = gerar(original, THUMB, "thumb-400.jpg", "THUMB", false, marcaDagua);
            Derivado web = gerar(original, WEB, "web-1600.jpg", "WEB", false, marcaDagua);
            Derivado thumbMarcada = gerar(original, THUMB, "thumb-400-marca.jpg",
                    "THUMB", true, marcaDagua);
            Derivado webMarcada = gerar(original, WEB, "web-1600-marca.jpg",
                    "WEB", true, marcaDagua);
            return new Resultado(original.getWidth(), original.getHeight(),
                    List.of(thumb, web, thumbMarcada, webMarcada));
        } catch (IOException ex) {
            throw new ArquivoInvalidoException("IMAGEM_INVALIDA",
                    "Falha ao processar a imagem: " + ex.getMessage());
        }
    }

    private static Derivado gerar(BufferedImage original, int limite, String nome,
                                   String tipo, boolean marcada, String marcaDagua)
            throws IOException {
        BufferedImage redimensionada = Thumbnails.of(original)
                .size(limite, limite)
                .asBufferedImage();
        BufferedImage rgb = new BufferedImage(
                redimensionada.getWidth(), redimensionada.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(redimensionada, 0, 0, null);
            if (marcada) {
                aplicarMarcaDagua(graphics, rgb.getWidth(), rgb.getHeight(), marcaDagua);
            }
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", saida);
        return new Derivado(tipo, nome, "image/jpeg", saida.toByteArray(),
                rgb.getWidth(), rgb.getHeight(), marcada);
    }

    /**
     * Aplica o nome do estudio em diagonal, repetido e com baixa opacidade.
     *
     * <p>O desenho e deterministico (sem aleatoriedade) para que os testes
     * consigam comparar a previa marcada com a limpa.
     */
    private static void aplicarMarcaDagua(Graphics2D graphics, int largura, int altura,
                                          String marcaDagua) {
        String texto = marcaDagua == null || marcaDagua.isBlank()
                ? MARCA_PADRAO : marcaDagua.trim();
        int tamanho = Math.max(12, Math.min(largura, altura) / 18);
        Font fonte = new Font(Font.SANS_SERIF, Font.BOLD, tamanho);

        Graphics2D marca = (Graphics2D) graphics.create();
        try {
            marca.setFont(fonte);
            marca.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            marca.rotate(Math.toRadians(-30), largura / 2.0, altura / 2.0);

            FontMetrics metrics = marca.getFontMetrics();
            int larguraTexto = metrics.stringWidth(texto);
            int passoX = Math.max(larguraTexto + tamanho * 2, tamanho * 5);
            int passoY = Math.max(tamanho * 5, 48);

            for (int y = -altura; y < altura * 2; y += passoY) {
                for (int x = -largura; x < largura * 2; x += passoX) {
                    marca.setColor(new Color(0, 0, 0, 42));
                    marca.drawString(texto, x + 1, y + 1);
                    marca.setColor(new Color(255, 255, 255, 122));
                    marca.drawString(texto, x, y);
                }
            }
        } finally {
            marca.dispose();
        }
    }
}
