package br.edu.bd2.modelo;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Endereco {
    @Column(nullable = false, length = 120)
    private String rua;
    @Column(nullable = false, length = 20)
    private String numero;
    @Column(length = 120)
    private String complemento;
    @Column(nullable = false, length = 80)
    private String bairro;
    @Column(nullable = false, length = 80)
    private String cidade;
    // O driver PostgreSQL reporta CHAR(n) como bpchar ao validador do Hibernate.
    @Column(nullable = false, length = 2, columnDefinition = "bpchar(2)")
    private String estado;
    @Column(nullable = false, length = 8)
    private String cep;

    protected Endereco() {
    }

    public Endereco(String rua, String numero, String complemento, String bairro,
                    String cidade, String estado, String cep) {
        this.rua = Objects.requireNonNull(rua, "Rua obrigatória");
        this.numero = Objects.requireNonNull(numero, "Número obrigatório");
        this.complemento = complemento;
        this.bairro = Objects.requireNonNull(bairro, "Bairro obrigatório");
        this.cidade = Objects.requireNonNull(cidade, "Cidade obrigatória");
        this.estado = Objects.requireNonNull(estado, "Estado obrigatório");
        this.cep = Objects.requireNonNull(cep, "CEP obrigatório");
    }

    public String getRua() {
        return rua;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    public String getCep() {
        return cep;
    }
}
