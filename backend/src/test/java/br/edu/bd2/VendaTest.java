package br.edu.bd2;

import br.edu.bd2.modelo.Categoria;
import br.edu.bd2.modelo.Cliente;
import br.edu.bd2.modelo.Departamento;
import br.edu.bd2.modelo.FormaPagamento;
import br.edu.bd2.modelo.Funcionario;
import br.edu.bd2.modelo.ItemVenda;
import br.edu.bd2.modelo.Medicamento;
import br.edu.bd2.modelo.Pagamento;
import br.edu.bd2.modelo.StatusVenda;
import br.edu.bd2.modelo.TipoControleMedicamento;
import br.edu.bd2.modelo.UnidadeFarmacia;
import br.edu.bd2.modelo.Venda;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static br.edu.bd2.DadosTeste.*;
import static org.junit.jupiter.api.Assertions.*;

class VendaTest {
    private final Date dia = data("2026-01-10T15:00:00Z");
    private final UnidadeFarmacia loja = unidade("Centro", "00000000000001");
    private final Cliente cliente = cliente("Alana", "00000000001", "F1");
    private final Funcionario atendente = funcionario("Luiza", "00000000002", "M1", loja);
    private final Departamento departamento = new Departamento("Medicamentos", null);
    private final Categoria categoria = new Categoria("Analgésicos", null, departamento);
    private final Medicamento dipirona = medicamento("Dipirona", "1", categoria, TipoControleMedicamento.ISENTO);

    @Test
    void calculaValoresEConservaPrecoDaVenda() {
        Venda venda = new Venda(cliente, loja, atendente, dia);
        ItemVenda item = venda.adicionarItem(1, dipirona, 2, dipirona.getPrecoBase(), valor("1.23"));
        venda.adicionarItem(2, dipirona, 1, valor("8.00"), valor("0"));
        assertEquals(valor("26.77"), venda.getValorTotal());
        venda.removerItem(2);
        dipirona.setPrecoBase(valor("99.00"));
        assertEquals(valor("10.00"), item.getPrecoUnitario());
        assertEquals(valor("18.77"), item.calcularValorLiquido());
        assertEquals(valor("18.77"), venda.calcularValorTotal());
        assertEquals(valor("18.77"), venda.registrarPagamento(FormaPagamento.PIX, dia, "TX1").getValor());
    }

    @Test
    void protegeInvariantesDoRegistro() {
        assertThrows(NullPointerException.class, () -> new Venda(null, loja, atendente, dia));
        assertThrows(NullPointerException.class, () -> new Venda(cliente, null, atendente, dia));
        assertThrows(NullPointerException.class, () -> new Venda(cliente, loja, null, dia));
        Venda venda = new Venda(cliente, loja, atendente, dia);
        assertThrows(IllegalStateException.class, () -> venda.registrarPagamento(FormaPagamento.PIX, dia, "TX1"));
        assertThrows(NullPointerException.class, () -> venda.adicionarItem(1, null, 1, valor("10"), valor("0")));
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(0, dipirona, 1, valor("10"), valor("0")));
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(1, dipirona, 0, valor("10"), valor("0")));
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(1, dipirona, 1, valor("10"), valor("11")));
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(1, dipirona, 1, valor("10.001"), valor("0")));
        assertEquals(valor("0.00"), venda.getValorTotal());
        venda.adicionarItem(1, dipirona, 1, valor("10"), valor("0"));
        assertThrows(IllegalArgumentException.class, () -> venda.adicionarItem(1, dipirona, 2, valor("10"), valor("0")));
        assertThrows(NullPointerException.class, () -> venda.registrarPagamento(null, dia, "TX1"));
        Pagamento pagamento = venda.registrarPagamento(FormaPagamento.PIX, dia, "TX1");
        assertThrows(IllegalStateException.class, () -> venda.removerItem(1));
        assertThrows(IllegalStateException.class, () -> venda.registrarPagamento(FormaPagamento.PIX, dia, "TX2"));
        pagamento.confirmar();
        assertTrue(pagamento.isAprovado());
        venda.cancelar();
        assertEquals(StatusVenda.CANCELADA, venda.getStatus());
        assertFalse(pagamento.isAprovado());
        assertThrows(IllegalStateException.class, pagamento::confirmar);
    }

    @Test
    void sincronizaAssociacoesEmMemoria() {
        Venda venda = new Venda(cliente, loja, atendente, dia);
        assertTrue(cliente.getVendas().contains(venda));
        assertTrue(atendente.getVendas().contains(venda));
        assertTrue(loja.getVendas().contains(venda));
        assertTrue(loja.getFuncionarios().contains(atendente));

        UnidadeFarmacia novaLoja = unidade("Sul", "00000000000002");
        atendente.setUnidade(novaLoja);
        assertFalse(loja.getFuncionarios().contains(atendente));
        assertTrue(novaLoja.getFuncionarios().contains(atendente));

        Departamento destino = new Departamento("Outro", null);
        categoria.setDepartamento(destino);
        assertFalse(departamento.getCategorias().contains(categoria));
        assertTrue(destino.getCategorias().contains(categoria));
        Categoria novaCategoria = new Categoria("Outra", null, destino);
        dipirona.setCategoria(novaCategoria);
        assertFalse(categoria.getMedicamentos().contains(dipirona));
        assertTrue(novaCategoria.getMedicamentos().contains(dipirona));
    }
}
