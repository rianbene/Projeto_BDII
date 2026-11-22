package br.edu.bd2.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "cliente")
@NamedQuery(name = "Cliente.comHistorico",
    query = "SELECT DISTINCT c FROM Cliente c JOIN c.vendas v ORDER BY c.nome, c.id")
public class Cliente extends Pessoa {
    @Column(name = "numero_cartao_fidelidade", nullable = false, unique = true, length = 30)
    private String numeroCartaoFidelidade;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_cadastro", nullable = false)
    private Date dataCadastro;
    @Column(nullable = false)
    private boolean ativo = true;
    @OneToMany(mappedBy = "cliente")
    private List<Venda> vendas = new ArrayList<>();

    protected Cliente() {
    }

    public Cliente(String nome, String cpf, Date dataNascimento, String telefone, String email,
                   Endereco endereco, String numeroCartaoFidelidade, Date dataCadastro) {
        super(nome, cpf, dataNascimento, telefone, email, endereco);
        this.numeroCartaoFidelidade = Objects.requireNonNull(numeroCartaoFidelidade, "Cartão obrigatório");
        this.dataCadastro = new Date(Objects.requireNonNull(dataCadastro, "Data de cadastro obrigatória").getTime());
    }

    public String getNumeroCartaoFidelidade() {
        return numeroCartaoFidelidade;
    }

    public Date getDataCadastro() {
        return new Date(dataCadastro.getTime());
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<Venda> getVendas() {
        return Collections.unmodifiableList(vendas);
    }

    void vincularVenda(Venda venda) {
        vendas.add(venda);
    }
}
