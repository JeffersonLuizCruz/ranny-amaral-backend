package com.ranny.fotografia.galeria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Item selecionado dentro de uma selecao. */
@Entity
@Table(name = "selecao_itens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SelecaoItem {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "selecao_id", nullable = false, length = 64)
    private String selecaoId;

    @Column(name = "midia_id", nullable = false, length = 64)
    private String midiaId;

    @Column(nullable = false, length = 10)
    private String tipo;

    public static SelecaoItem criar(String selecaoId, String midiaId, MidiaTipo tipo) {
        SelecaoItem item = new SelecaoItem();
        item.id = UUID.randomUUID().toString();
        item.selecaoId = selecaoId;
        item.midiaId = midiaId;
        item.tipo = tipo.name();
        return item;
    }
}
