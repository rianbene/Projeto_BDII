package br.edu.bd2.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "categoria", uniqueConstraints = @UniqueConstraint(columnNames = {"departamento_id", "nome"}))
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String nome;
    @Column(length = 255)
    private String descricao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;
    @OneToMany(mappedBy = "categoria")
    private List<Medicamento> medicamentos = new ArrayList<>();

    protected Categoria() {
    }

    public Categoria(String nome, String descricao, Departamento departamento) {
        setNome(nome);
        setDescricao(descricao);
        setDepartamento(departamento);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Objects.requireNonNull(nome, "Nome obrigatório");
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento novoDepartamento) {
        Objects.requireNonNull(novoDepartamento, "Departamento obrigatório");
        if (departamento == novoDepartamento) {
            return;
        }
        if (Associacoes.colecaoCarregada(departamento, "categorias")) {
            departamento.desvincularCategoria(this);
        }
        departamento = novoDepartamento;
        if (Associacoes.colecaoCarregada(departamento, "categorias")) {
            departamento.vincularCategoria(this);
        }
    }

    public List<Medicamento> getMedicamentos() {
        return Collections.unmodifiableList(medicamentos);
    }

    void vincularMedicamento(Medicamento medicamento) {
        medicamentos.add(medicamento);
    }

    void desvincularMedicamento(Medicamento medicamento) {
        medicamentos.remove(medicamento);
    }
}
