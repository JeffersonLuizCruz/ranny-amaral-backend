package com.ranny.fotografia.galeria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Derivado de uma midia (miniatura ou versao web). */
@Entity
@Table(name = "midia_derivados")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MidiaDerivado {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "midia_id", nullable = false, length = 64)
    private String midiaId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "arquivo_id", nullable = false, length = 64)
    private String arquivoId;

    private Integer largura;

    private Integer altura;

    @Column(length = 20)
    private String formato;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public static MidiaDerivado criar(String midiaId, String tipo, String arquivoId,
                                      int largura, int altura, String formato) {
        MidiaDerivado derivado = new MidiaDerivado();
        derivado.id = UUID.randomUUID().toString();
        derivado.midiaId = midiaId;
        derivado.tipo = tipo;
        derivado.arquivoId = arquivoId;
        derivado.largura = largura;
        derivado.altura = altura;
        derivado.formato = formato;
        derivado.criadoEm = Instant.now();
        return derivado;
    }
}
