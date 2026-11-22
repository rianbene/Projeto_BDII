package br.edu.bd2.modelo;

import java.math.BigDecimal;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "item_venda")
public class ItemVenda {
    @EmbeddedId
    private ItemVendaId id;
    @MapsId("vendaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;
    @Column(nullable = false)
    private int quantidade;
    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;
    @Column(name = "valor_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquido;

    protected ItemVenda() {
    }

    ItemVenda(Venda venda, int numeroItem, Medicamento medicamento, int quantidade,
              BigDecimal precoUnitario, BigDecimal desconto) {
        this.venda = Objects.requireNonNull(venda, "Venda obrigatória");
        this.id = new ItemVendaId(venda.getId(), numeroItem);
        this.medicamento = Objects.requireNonNull(medicamento, "Medicamento obrigatório");
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        this.quantidade = quantidade;
        this.precoUnitario = ValorMonetario.normalizar(precoUnitario);
        this.desconto = ValorMonetario.normalizar(desconto);
        this.valorLiquido = calcularValorLiquido();
    }

    public ItemVendaId getId() {
        return id;
    }

    public Integer getNumeroItem() {
        return id.getNumeroItem();
    }

    public Venda getVenda() {
        return venda;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }

    public BigDecimal calcularValorLiquido() {
        BigDecimal bruto = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        if (desconto.compareTo(bruto) > 0) {
            throw new IllegalArgumentException("Desconto superior ao valor bruto do item");
        }
        return ValorMonetario.normalizar(bruto.subtract(desconto));
    }
}
