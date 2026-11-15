package br.edu.bd2.dao;

import java.util.Date;
import java.util.Objects;

final class Periodo {
    private Periodo() {
    }

    static void validarObrigatorio(Date inicio, Date fimExclusivo) {
        Objects.requireNonNull(inicio, "Início do período obrigatório");
        Objects.requireNonNull(fimExclusivo, "Fim do período obrigatório");
        validarLimites(inicio, fimExclusivo);
    }

    static void validarLimites(Date inicio, Date fimExclusivo) {
        if (inicio != null && fimExclusivo != null && !inicio.before(fimExclusivo)) {
            throw new IllegalArgumentException("O início deve ser anterior ao fim do período");
        }
    }
}
