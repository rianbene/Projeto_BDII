package br.edu.bd2.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "departamento")
public class Departamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String nome;
    @Column(length = 255)
    private String descricao;
    @OneToMany(mappedBy = "departamento")
    private List<Categoria> categorias = new ArrayList<>();

    protected Departamento() {
    }

    public Departamento(String nome, String descricao) {
        setNome(nome);
        setDescricao(descricao);
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

    public List<Categoria> getCategorias() {
        return Collections.unmodifiableList(categorias);
    }

    void vincularCategoria(Categoria categoria) {
        categorias.add(categoria);
    }

    void desvincularCategoria(Categoria categoria) {
        categorias.remove(categoria);
    }
}
