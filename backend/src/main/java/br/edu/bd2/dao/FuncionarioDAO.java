package br.edu.bd2.dao;

import br.edu.bd2.modelo.CargoFuncionario;
import br.edu.bd2.modelo.Funcionario;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

public class FuncionarioDAO extends GenericDAO<Funcionario, Long> {
    public FuncionarioDAO() {
        super(Funcionario.class);
    }

    public FuncionarioDAO(EntityManager em) {
        super(Funcionario.class, em);
    }

    public Optional<Funcionario> buscarPorMatricula(String matricula) {
        return ler(em -> em.createQuery(
                "SELECT f FROM Funcionario f WHERE f.matricula = :matricula", Funcionario.class)
                .setParameter("matricula", matricula)
                .getResultList().stream().findFirst());
    }

    public List<Funcionario> listarPorUnidade(Long unidadeId) {
        return ler(em -> em.createQuery(
                "SELECT f FROM Funcionario f WHERE f.unidade.id = :id ORDER BY f.nome, f.id", Funcionario.class)
                .setParameter("id", unidadeId)
                .getResultList());
    }

    public List<Funcionario> listarPorCargo(CargoFuncionario cargo) {
        return ler(em -> em.createQuery(
                "SELECT f FROM Funcionario f WHERE f.cargo = :cargo ORDER BY f.nome, f.id", Funcionario.class)
                .setParameter("cargo", cargo)
                .getResultList());
    }
}
