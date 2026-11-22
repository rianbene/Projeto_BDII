package br.edu.bd2.modelo;

import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "pessoa")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pessoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_nascimento", nullable = false)
    private Date dataNascimento;
    @Column(nullable = false, length = 20)
    private String telefone;
    @Column(nullable = false, unique = true, length = 120)
    private String email;
    @Embedded
    private Endereco endereco;

    protected Pessoa() {
    }

    protected Pessoa(String nome, String cpf, Date dataNascimento, String telefone,
                     String email, Endereco endereco) {
        this.nome = Objects.requireNonNull(nome, "Nome obrigatório");
        this.cpf = Objects.requireNonNull(cpf, "CPF obrigatório");
        this.dataNascimento = new Date(Objects.requireNonNull(dataNascimento, "Nascimento obrigatório").getTime());
        setTelefone(telefone);
        setEmail(email);
        setEndereco(endereco);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public Date getDataNascimento() {
        return new Date(dataNascimento.getTime());
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = Objects.requireNonNull(telefone, "Telefone obrigatório");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "E-mail obrigatório");
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = Objects.requireNonNull(endereco, "Endereço obrigatório");
    }
}
