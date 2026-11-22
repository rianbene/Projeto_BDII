package br.edu.bd2.dao;

import br.edu.bd2.modelo.Departamento;
import javax.persistence.EntityManager;

public class DepartamentoDAO extends GenericDAO<Departamento, Long> {
    public DepartamentoDAO() {
        super(Departamento.class);
    }

    public DepartamentoDAO(EntityManager em) {
        super(Departamento.class, em);
    }
}
