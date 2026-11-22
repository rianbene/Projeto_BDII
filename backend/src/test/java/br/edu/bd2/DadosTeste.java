package br.edu.bd2;

import br.edu.bd2.modelo.CargoFuncionario;
import br.edu.bd2.modelo.Categoria;
import br.edu.bd2.modelo.Cliente;
import br.edu.bd2.modelo.Endereco;
import br.edu.bd2.modelo.Funcionario;
import br.edu.bd2.modelo.Medicamento;
import br.edu.bd2.modelo.TipoControleMedicamento;
import br.edu.bd2.modelo.UnidadeFarmacia;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

final class DadosTeste {
    private DadosTeste() {
    }

    static Date data(String instante) {
        return Date.from(Instant.parse(instante));
    }

    static BigDecimal valor(String numero) {
        return new BigDecimal(numero);
    }

    static Endereco endereco() {
        return new Endereco("Rua das Flores", "123", null, "Centro", "Recife", "PE", "50000000");
    }

    static UnidadeFarmacia unidade(String nome, String cnpj) {
        return new UnidadeFarmacia(nome, cnpj, "8130000000", endereco());
    }

    static Cliente cliente(String nome, String cpf, String cartao) {
        return new Cliente(nome, cpf, data("1990-03-04T12:00:00Z"), "81900000000",
                nome + "@exemplo.test", endereco(), cartao, data("2025-01-01T12:00:00Z"));
    }

    static Funcionario funcionario(String nome, String cpf, String matricula, UnidadeFarmacia unidade) {
        return new Funcionario(nome, cpf, data("1994-07-05T12:00:00Z"), "81900000001",
                nome + "@exemplo.test", endereco(), matricula, data("2025-02-01T12:00:00Z"), valor("2000.00"),
                CargoFuncionario.FARMACEUTICO, unidade);
    }

    static Medicamento medicamento(String nome, String codigo, Categoria categoria,
                                    TipoControleMedicamento tipo) {
        return new Medicamento(nome, nome, codigo, "Laboratório", "Caixa", valor("10.00"), 100, categoria, tipo);
    }
}
