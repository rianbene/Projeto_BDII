package br.edu.bd2.dao;

import br.edu.bd2.modelo.Categoria;
import java.util.List;
import javax.persistence.EntityManager;

public class CategoriaDAO extends GenericDAO<Categoria, Long> {
    public CategoriaDAO() {
        super(Categoria.class);
    }

    public CategoriaDAO(EntityManager em) {
        super(Categoria.class, em);
    }

    public List<Categoria> listarPorDepartamento(Long departamentoId) {
        return ler(em -> em.createQuery(
                "SELECT c FROM Categoria c WHERE c.departamento.id = :id ORDER BY c.nome, c.id", Categoria.class)
                .setParameter("id", departamentoId)
                .getResultList());
    }
}
