package br.edu.bd2.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "venda")
@NamedQuery(name = "Venda.porCliente",
    query = "SELECT v FROM Venda v JOIN v.cliente c WHERE c.id = :clienteId ORDER BY v.dataHora DESC, v.id DESC")
public class Venda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_hora", nullable = false)
    private Date dataHora;
    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = ValorMonetario.ZERO;
    @Column(columnDefinition = "text")
    private String observacao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVenda status = StatusVenda.CONCLUIDA;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeFarmacia unidade;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.numeroItem")
    private List<ItemVenda> itens = new ArrayList<>();
    // Com optional=false no lado inverso, Hibernate 5.2 insere Pagamento antes de Venda.
    @OneToOne(mappedBy = "venda", fetch = FetchType.EAGER,
        cascade = CascadeType.ALL, orphanRemoval = true)
    private Pagamento pagamento;

    protected Venda() {
    }

    public Venda(Cliente cliente, UnidadeFarmacia unidade, Funcionario funcionario, Date dataHora) {
        this.cliente = Objects.requireNonNull(cliente, "Cliente obrigatório");
        this.unidade = Objects.requireNonNull(unidade, "Unidade obrigatória");
        this.funcionario = Objects.requireNonNull(funcionario, "Funcionário obrigatório");
        this.dataHora = new Date(Objects.requireNonNull(dataHora, "Data da venda obrigatória").getTime());
        if (Associacoes.colecaoCarregada(cliente, "vendas")) {
            cliente.vincularVenda(this);
        }
        if (Associacoes.colecaoCarregada(unidade, "vendas")) {
            unidade.vincularVenda(this);
        }
        if (Associacoes.colecaoCarregada(funcionario, "vendas")) {
            funcionario.vincularVenda(this);
        }
    }

    public Long getId() {
        return id;
    }

    public Date getDataHora() {
        return new Date(dataHora.getTime());
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public UnidadeFarmacia getUnidade() {
        return unidade;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public ItemVenda adicionarItem(int numeroItem, Medicamento medicamento, int quantidade,
                                   BigDecimal precoUnitario, BigDecimal desconto) {
        verificarEdicaoDosItens();
        if (itens.stream().anyMatch(item -> item.getNumeroItem() == numeroItem)) {
            throw new IllegalArgumentException("Número de item já usado nesta venda: " + numeroItem);
        }
        ItemVenda item = new ItemVenda(this, numeroItem, medicamento, quantidade, precoUnitario, desconto);
        valorTotal = ValorMonetario.normalizar(valorTotal.add(item.getValorLiquido()));
        itens.add(item);
        return item;
    }

    public void removerItem(int numeroItem) {
        verificarEdicaoDosItens();
        itens.removeIf(item -> item.getNumeroItem() == numeroItem);
        valorTotal = calcularValorTotal();
    }

    private void verificarEdicaoDosItens() {
        if (id != null || pagamento != null) {
            throw new IllegalStateException("Os itens devem ser definidos antes do pagamento");
        }
    }

    public BigDecimal calcularValorTotal() {
        return itens.stream().map(ItemVenda::getValorLiquido).reduce(ValorMonetario.ZERO, BigDecimal::add);
    }

    public Pagamento registrarPagamento(FormaPagamento forma, Date dataHora, String numeroTransacao) {
        if (id != null || pagamento != null || itens.isEmpty()) {
            throw new IllegalStateException("O pagamento é único e exige uma venda com itens definida");
        }
        pagamento = new Pagamento(this, forma, dataHora, numeroTransacao);
        return pagamento;
    }

    public void cancelar() {
        if (pagamento == null) {
            throw new IllegalStateException("Finalize o registro do pagamento antes de cancelar a venda");
        }
        pagamento.cancelar();
        status = StatusVenda.CANCELADA;
    }

    @PrePersist
    private void validarVendaCompleta() {
        if (itens.isEmpty()) {
            throw new IllegalStateException("A venda deve possuir pelo menos um item");
        }
        if (pagamento == null) {
            throw new IllegalStateException("A venda deve possuir um pagamento");
        }
    }
}
