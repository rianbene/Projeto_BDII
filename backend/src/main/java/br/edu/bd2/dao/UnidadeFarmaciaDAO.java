package br.edu.bd2.dao;

import br.edu.bd2.modelo.UnidadeFarmacia;
import javax.persistence.EntityManager;

public class UnidadeFarmaciaDAO extends GenericDAO<UnidadeFarmacia, Long> {
    public UnidadeFarmaciaDAO() {
        super(UnidadeFarmacia.class);
    }

    public UnidadeFarmaciaDAO(EntityManager em) {
        super(UnidadeFarmacia.class, em);
    }
}
