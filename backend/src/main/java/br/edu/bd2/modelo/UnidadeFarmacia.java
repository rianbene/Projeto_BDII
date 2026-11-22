package br.edu.bd2.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "unidade_farmacia")
public class UnidadeFarmacia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;
    @Column(nullable = false, length = 20)
    private String telefone;
    @Column(nullable = false)
    private boolean ativa = true;
    @Embedded
    private Endereco endereco;
    @OneToMany(mappedBy = "unidade")
    private List<Funcionario> funcionarios = new ArrayList<>();
    @OneToMany(mappedBy = "unidade")
    private List<Venda> vendas = new ArrayList<>();

    protected UnidadeFarmacia() {
    }

    public UnidadeFarmacia(String nome, String cnpj, String telefone, Endereco endereco) {
        setNome(nome);
        this.cnpj = Objects.requireNonNull(cnpj, "CNPJ obrigatório");
        setTelefone(telefone);
        setEndereco(endereco);
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

    public String getCnpj() {
        return cnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = Objects.requireNonNull(telefone, "Telefone obrigatório");
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = Objects.requireNonNull(endereco, "Endereço obrigatório");
    }

    public List<Funcionario> getFuncionarios() {
        return Collections.unmodifiableList(funcionarios);
    }

    public List<Venda> getVendas() {
        return Collections.unmodifiableList(vendas);
    }

    void vincularFuncionario(Funcionario funcionario) {
        funcionarios.add(funcionario);
    }

    void desvincularFuncionario(Funcionario funcionario) {
        funcionarios.remove(funcionario);
    }

    void vincularVenda(Venda venda) {
        vendas.add(venda);
    }
}
