package br.edu.bd2.dao;

import br.edu.bd2.modelo.Cliente;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

public class ClienteDAO extends GenericDAO<Cliente, Long> {
    public ClienteDAO() {
        super(Cliente.class);
    }

    public ClienteDAO(EntityManager em) {
        super(Cliente.class, em);
    }

    public Optional<Cliente> buscarPorCpf(String cpf) {
        return ler(em -> em.createQuery("SELECT c FROM Cliente c WHERE c.cpf = :cpf", Cliente.class)
                .setParameter("cpf", cpf)
                .getResultList().stream().findFirst());
    }

    public Optional<Cliente> buscarPorCartaoFidelidade(String cartao) {
        return ler(em -> em.createQuery(
                "SELECT c FROM Cliente c WHERE c.numeroCartaoFidelidade = :cartao", Cliente.class)
                .setParameter("cartao", cartao)
                .getResultList().stream().findFirst());
    }

    public List<Cliente> listarComHistorico() {
        return ler(em -> em.createNamedQuery("Cliente.comHistorico", Cliente.class).getResultList());
    }
}
