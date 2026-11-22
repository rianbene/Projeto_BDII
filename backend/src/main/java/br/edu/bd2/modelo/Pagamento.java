package br.edu.bd2.modelo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "pagamento")
public class Pagamento {
    @Id
    @Column(name = "venda_id")
    private Long id;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false, unique = true)
    private Venda venda;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_hora", nullable = false)
    private Date dataHora;
    @Column(name = "numero_transacao", unique = true, length = 60)
    private String numeroTransacao;
    @Column(nullable = false)
    private boolean aprovado;
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    protected Pagamento() {
    }

    Pagamento(Venda venda, FormaPagamento formaPagamento, Date dataHora, String numeroTransacao) {
        this.venda = Objects.requireNonNull(venda, "Venda obrigatória");
        this.valor = venda.getValorTotal();
        this.dataHora = new Date(Objects.requireNonNull(dataHora, "Data do pagamento obrigatória").getTime());
        this.numeroTransacao = numeroTransacao == null || numeroTransacao.trim().isEmpty()
                ? null : numeroTransacao;
        this.formaPagamento = Objects.requireNonNull(formaPagamento, "Forma de pagamento obrigatória");
    }

    public Long getId() {
        return id;
    }

    public Venda getVenda() {
        return venda;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Date getDataHora() {
        return new Date(dataHora.getTime());
    }

    public String getNumeroTransacao() {
        return numeroTransacao;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void confirmar() {
        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new IllegalStateException("Venda cancelada não pode ter pagamento confirmado");
        }
        aprovado = true;
    }

    public void cancelar() {
        aprovado = false;
    }
}
