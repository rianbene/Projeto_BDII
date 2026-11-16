package br.edu.bd2.modelo;

import java.math.BigDecimal;
import java.util.Objects;
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
import javax.persistence.Table;

@Entity
@Table(name = "medicamento")
public class Medicamento {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(name = "principio_ativo", nullable = false, length = 120)
    private String principioAtivo;
    @Column(name = "codigo_barras", nullable = false, unique = true, length = 30)
    private String codigoBarras;
    @Column(nullable = false, length = 120)
    private String laboratorio;
    @Column(nullable = false, length = 80)
    private String apresentacao;
    @Column(name = "preco_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoBase;
    @Column(nullable = false)
    private int estoque;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_controle", nullable = false, length = 20)
    private TipoControleMedicamento tipoControle;

    protected Medicamento() {
    }

    public Medicamento(String nome, String principioAtivo, String codigoBarras, String laboratorio,
                       String apresentacao, BigDecimal precoBase, int estoque, Categoria categoria,
                       TipoControleMedicamento tipoControle) {
        this.nome = Objects.requireNonNull(nome, "Nome obrigatório");
        this.principioAtivo = Objects.requireNonNull(principioAtivo, "Princípio ativo obrigatório");
        this.codigoBarras = Objects.requireNonNull(codigoBarras, "Código de barras obrigatório");
        this.laboratorio = Objects.requireNonNull(laboratorio, "Laboratório obrigatório");
        this.apresentacao = Objects.requireNonNull(apresentacao, "Apresentação obrigatória");
        setPrecoBase(precoBase);
        setEstoque(estoque);
        setTipoControle(tipoControle);
        setCategoria(categoria);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getPrincipioAtivo() {
        return principioAtivo;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public String getApresentacao() {
        return apresentacao;
    }

    public BigDecimal getPrecoBase() {
        return precoBase;
    }

    public void setPrecoBase(BigDecimal precoBase) {
        this.precoBase = ValorMonetario.normalizar(precoBase);
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        if (estoque < 0) {
            throw new IllegalArgumentException("Estoque deve ser maior ou igual a zero");
        }
        this.estoque = estoque;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria novaCategoria) {
        Objects.requireNonNull(novaCategoria, "Categoria obrigatória");
        if (categoria == novaCategoria) {
            return;
        }
        if (Associacoes.colecaoCarregada(categoria, "medicamentos")) {
            categoria.desvincularMedicamento(this);
        }
        categoria = novaCategoria;
        if (Associacoes.colecaoCarregada(categoria, "medicamentos")) {
            categoria.vincularMedicamento(this);
        }
    }

    public TipoControleMedicamento getTipoControle() {
        return tipoControle;
    }

    public void setTipoControle(TipoControleMedicamento tipoControle) {
        this.tipoControle = Objects.requireNonNull(tipoControle, "Tipo de controle obrigatório");
    }
}
