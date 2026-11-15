package br.edu.bd2.dao;

import br.edu.bd2.modelo.Medicamento;
import br.edu.bd2.modelo.StatusVenda;
import br.edu.bd2.modelo.TipoControleMedicamento;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class MedicamentoDAO extends GenericDAO<Medicamento, Long> {
    public MedicamentoDAO() {
        super(Medicamento.class);
    }

    public MedicamentoDAO(EntityManager em) {
        super(Medicamento.class, em);
    }

    public Optional<Medicamento> buscarPorCodigoBarras(String codigo) {
        return ler(em -> em.createQuery(
                "SELECT m FROM Medicamento m WHERE m.codigoBarras = :codigo", Medicamento.class)
                .setParameter("codigo", codigo)
                .getResultList().stream().findFirst());
    }

    /** Cada linha contém Long medicamentoId, String nome e Long quantidade de unidades. */
    public List<Object[]> listarMaisVendidos() {
        return ler(em -> em.createQuery(
                "SELECT m.id, m.nome, SUM(i.quantidade) FROM ItemVenda i JOIN i.medicamento m "
                    + "WHERE i.venda.status = :status GROUP BY m.id, m.nome "
                    + "ORDER BY SUM(i.quantidade) DESC, m.id", Object[].class)
                .setParameter("status", StatusVenda.CONCLUIDA)
                .getResultList());
    }

    public List<Medicamento> listarNuncaVendidos() {
        return ler(em -> em.createQuery(
                "SELECT m FROM Medicamento m WHERE NOT EXISTS "
                    + "(SELECT i.id.numeroItem FROM ItemVenda i WHERE i.medicamento = m "
                    + "AND i.venda.status = :status) ORDER BY m.nome, m.id", Medicamento.class)
                .setParameter("status", StatusVenda.CONCLUIDA)
                .getResultList());
    }

    public List<Medicamento> buscarComFiltros(Long categoriaId, Long departamentoId,
                                            TipoControleMedicamento tipoControle) {
        return ler(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Medicamento> consulta = cb.createQuery(Medicamento.class);
            Root<Medicamento> medicamento = consulta.from(Medicamento.class);
            List<Predicate> filtros = new ArrayList<>();
            if (categoriaId != null) {
                filtros.add(cb.equal(medicamento.get("categoria").get("id"), categoriaId));
            }
            if (departamentoId != null) {
                filtros.add(cb.equal(medicamento.get("categoria").get("departamento").get("id"), departamentoId));
            }
            if (tipoControle != null) {
                filtros.add(cb.equal(medicamento.get("tipoControle"), tipoControle));
            }
            consulta.select(medicamento).where(filtros.toArray(new Predicate[0]))
                    .orderBy(cb.asc(medicamento.get("nome")), cb.asc(medicamento.get("id")));
            return em.createQuery(consulta).getResultList();
        });
    }
}
