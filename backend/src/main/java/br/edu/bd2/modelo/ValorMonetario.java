package br.edu.bd2.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class ValorMonetario {
    static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAXIMO = new BigDecimal("9999999999.99");

    private ValorMonetario() {
    }

    static BigDecimal normalizar(BigDecimal valor) {
        Objects.requireNonNull(valor, "Valor monetário obrigatório");
        if (valor.signum() < 0 || valor.compareTo(MAXIMO) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Valor fora dos limites de NUMERIC(12,2): " + valor);
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }
}
