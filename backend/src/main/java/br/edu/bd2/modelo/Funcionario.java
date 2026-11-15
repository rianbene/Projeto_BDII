package br.edu.bd2.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "funcionario")
public class Funcionario extends Pessoa {
    @Column(nullable = false, unique = true, length = 30)
    private String matricula;
    @Temporal(TemporalType.DATE)
    @Column(name = "data_admissao", nullable = false)
    private Date dataAdmissao;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salario;
    @Column(nullable = false)
    private boolean ativo = true;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CargoFuncionario cargo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeFarmacia unidade;
    @OneToMany(mappedBy = "funcionario")
    private List<Venda> vendas = new ArrayList<>();

    protected Funcionario() {
    }

    public Funcionario(String nome, String cpf, Date dataNascimento, String telefone, String email,
                       Endereco endereco, String matricula, Date dataAdmissao, BigDecimal salario,
                       CargoFuncionario cargo, UnidadeFarmacia unidade) {
        super(nome, cpf, dataNascimento, telefone, email, endereco);
        this.matricula = Objects.requireNonNull(matricula, "Matrícula obrigatória");
        this.dataAdmissao = new Date(Objects.requireNonNull(dataAdmissao, "Admissão obrigatória").getTime());
        setSalario(salario);
        setCargo(cargo);
        setUnidade(unidade);
    }

    public String getMatricula() {
        return matricula;
    }

    public Date getDataAdmissao() {
        return new Date(dataAdmissao.getTime());
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = ValorMonetario.normalizar(salario);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public CargoFuncionario getCargo() {
        return cargo;
    }

    public void setCargo(CargoFuncionario cargo) {
        this.cargo = Objects.requireNonNull(cargo, "Cargo obrigatório");
    }

    public UnidadeFarmacia getUnidade() {
        return unidade;
    }

    public void setUnidade(UnidadeFarmacia novaUnidade) {
        Objects.requireNonNull(novaUnidade, "Unidade obrigatória");
        if (unidade == novaUnidade) {
            return;
        }
        if (Associacoes.colecaoCarregada(unidade, "funcionarios")) {
            unidade.desvincularFuncionario(this);
        }
        unidade = novaUnidade;
        if (Associacoes.colecaoCarregada(unidade, "funcionarios")) {
            unidade.vincularFuncionario(this);
        }
    }

    public List<Venda> getVendas() {
        return Collections.unmodifiableList(vendas);
    }

    void vincularVenda(Venda venda) {
        vendas.add(venda);
    }
}
