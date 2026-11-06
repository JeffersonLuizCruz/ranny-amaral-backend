package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * PATTERN: Strategy (Converter) registrado como bean.
 *
 * <p>O Spring MVC converte enums de query string por {@code Enum.valueOf}, que
 * exige o nome exato da constante (maiusculo). O frontend envia os valores em
 * minusculo. Este converter reutiliza o {@code @JsonCreator} do enum, mantendo
 * um unico criterio de traducao para JSON e para parametros de URL.
 */
@Component
public class StatusAgendamentoConverter implements Converter<String, StatusAgendamento> {

    @Override
    public StatusAgendamento convert(String origem) {
        return StatusAgendamento.de(origem);
    }
}
