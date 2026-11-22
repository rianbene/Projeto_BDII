package br.edu.bd2.modelo;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ItemVendaId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "venda_id", nullable = false)
    private Long vendaId;
    @Column(name = "numero_item", nullable = false)
    private Integer numeroItem;

    protected ItemVendaId() {
    }

    public ItemVendaId(Long vendaId, int numeroItem) {
        if (numeroItem <= 0) {
            throw new IllegalArgumentException("Número do item deve ser positivo");
        }
        this.vendaId = vendaId;
        this.numeroItem = numeroItem;
    }

    public Long getVendaId() {
        return vendaId;
    }

    public Integer getNumeroItem() {
        return numeroItem;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof ItemVendaId)) {
            return false;
        }
        ItemVendaId chave = (ItemVendaId) outro;
        return Objects.equals(vendaId, chave.vendaId) && Objects.equals(numeroItem, chave.numeroItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendaId, numeroItem);
    }
}
