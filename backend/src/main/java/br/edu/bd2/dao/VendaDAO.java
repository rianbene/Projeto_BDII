package br.edu.bd2.dao;

import br.edu.bd2.modelo.StatusVenda;
import br.edu.bd2.modelo.UnidadeFarmacia;
import br.edu.bd2.modelo.Venda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.TemporalType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class VendaDAO extends GenericDAO<Venda, Long> {
    public VendaDAO() {
        super(Venda.class);
    }

    public VendaDAO(EntityManager em) {
        super(Venda.class, em);
    }

    public boolean cancelar(Long id) {
        return excluirPorId(id);
    }

    @Override
    protected boolean excluir(EntityManager em, Long id) {
        Venda venda = em.find(Venda.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (venda == null) {
            return false;
        }
        if (statusNoBanco(em, id) != StatusVenda.CANCELADA) {
            // O pagamento de uma venda nova pode estar aguardando a gravação.
            em.flush();
        }
        em.refresh(venda);
        venda.cancelar();
        return true;
    }

    @Override
    protected Venda mesclar(EntityManager em, Venda venda) {
        if (venda.getId() == null) {
            throw new IllegalArgumentException("Use salvar para registrar uma venda nova");
        }
        Venda atual = em.find(Venda.class, venda.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (atual == null) {
            throw new IllegalArgumentException("Venda não encontrada: " + venda.getId());
        }
        if (venda.getStatus() != StatusVenda.CANCELADA
                && (atual.getStatus() == StatusVenda.CANCELADA
                    || statusNoBanco(em, venda.getId()) == StatusVenda.CANCELADA)) {
            throw new IllegalStateException("Venda cancelada não pode ser reaberta");
        }
        return super.mesclar(em, venda);
    }

    private StatusVenda statusNoBanco(EntityManager em, Long id) {
        // Consulte o status sob o lock sem gravar o estado possivelmente antigo do contexto.
        return em.createQuery("SELECT v.status FROM Venda v WHERE v.id = :id", StatusVenda.class)
                .setParameter("id", id)
                .setFlushMode(FlushModeType.COMMIT)
                .getSingleResult();
    }

    public List<Venda> listarPorCliente(Long clienteId) {
        return ler(em -> em.createNamedQuery("Venda.porCliente", Venda.class)
                .setParameter("clienteId", clienteId)
                .getResultList());
    }

    public List<Venda> listarComItensPorCliente(Long clienteId) {
        return ler(em -> em.createQuery(
                "SELECT DISTINCT v FROM Venda v JOIN FETCH v.itens JOIN FETCH v.pagamento "
                    + "WHERE v.cliente.id = :clienteId ORDER BY v.dataHora DESC, v.id DESC", Venda.class)
                .setParameter("clienteId", clienteId)
                .getResultList());
    }

    /** Cada linha contém Long clienteId, String nome e BigDecimal total. */
    public List<Object[]> totalPorCliente(BigDecimal minimo) {
        Objects.requireNonNull(minimo, "Valor mínimo obrigatório");
        return ler(em -> em.createQuery(
                "SELECT c.id, c.nome, SUM(v.valorTotal) FROM Venda v JOIN v.cliente c "
                    + "WHERE v.status = :status GROUP BY c.id, c.nome "
                    + "HAVING SUM(v.valorTotal) >= :minimo ORDER BY SUM(v.valorTotal) DESC, c.id",
                Object[].class)
                .setParameter("status", StatusVenda.CONCLUIDA)
                .setParameter("minimo", minimo)
                .getResultList());
    }

    /** Cada linha contém Long unidadeId, String nome e BigDecimal faturamento. */
    public List<Object[]> faturamentoPorUnidade(Date inicio, Date fimExclusivo) {
        Periodo.validarObrigatorio(inicio, fimExclusivo);
        return ler(em -> em.createQuery(
                "SELECT u.id, u.nome, SUM(v.valorTotal) FROM Venda v JOIN v.unidade u "
                    + "WHERE v.status = :status AND v.dataHora >= :inicio AND v.dataHora < :fim "
                    + "GROUP BY u.id, u.nome ORDER BY u.id", Object[].class)
                .setParameter("status", StatusVenda.CONCLUIDA)
                .setParameter("inicio", inicio, TemporalType.TIMESTAMP)
                .setParameter("fim", fimExclusivo, TemporalType.TIMESTAMP)
                .getResultList());
    }

    public List<Venda> listarPorStatusEPeriodo(StatusVenda status, Date inicio, Date fimExclusivo) {
        Objects.requireNonNull(status, "Status obrigatório");
        Periodo.validarObrigatorio(inicio, fimExclusivo);
        return ler(em -> em.createQuery(
                "SELECT v FROM Venda v WHERE v.status = :status AND v.dataHora >= :inicio "
                    + "AND v.dataHora < :fim ORDER BY v.dataHora DESC, v.id DESC", Venda.class)
                .setParameter("status", status)
                .setParameter("inicio", inicio, TemporalType.TIMESTAMP)
                .setParameter("fim", fimExclusivo, TemporalType.TIMESTAMP)
                .getResultList());
    }

    public List<Venda> listarPaginadas(int deslocamento, int tamanho) {
        if (deslocamento < 0 || tamanho <= 0) {
            throw new IllegalArgumentException("Use deslocamento não negativo e tamanho positivo");
        }
        return ler(em -> em.createQuery(
                "SELECT v FROM Venda v ORDER BY v.dataHora DESC, v.id DESC", Venda.class)
                .setFirstResult(deslocamento)
                .setMaxResults(tamanho)
                .getResultList());
    }

    public List<Venda> buscarComFiltros(Long clienteId, Long unidadeId, Long funcionarioId,
                                      StatusVenda status, Date inicio, Date fimExclusivo) {
        Periodo.validarLimites(inicio, fimExclusivo);
        return ler(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Venda> consulta = cb.createQuery(Venda.class);
            Root<Venda> venda = consulta.from(Venda.class);
            Path<Date> data = venda.get("dataHora");
            List<Predicate> filtros = new ArrayList<>();
            if (clienteId != null) {
                filtros.add(cb.equal(venda.get("cliente").get("id"), clienteId));
            }
            if (unidadeId != null) {
                filtros.add(cb.equal(venda.get("unidade").get("id"), unidadeId));
            }
            if (funcionarioId != null) {
                filtros.add(cb.equal(venda.get("funcionario").get("id"), funcionarioId));
            }
            if (status != null) {
                filtros.add(cb.equal(venda.get("status"), status));
            }
            if (inicio != null) {
                filtros.add(cb.greaterThanOrEqualTo(data, inicio));
            }
            if (fimExclusivo != null) {
                filtros.add(cb.lessThan(data, fimExclusivo));
            }
            consulta.select(venda).where(filtros.toArray(new Predicate[0]))
                    .orderBy(cb.desc(data), cb.desc(venda.get("id")));
            return em.createQuery(consulta).getResultList();
        });
    }

    /** Cada linha contém Long unidadeId, String nome e Long quantidade de vendas. */
    public List<Object[]> contarConcluidasPorUnidade(Date inicio, Date fimExclusivo) {
        Periodo.validarObrigatorio(inicio, fimExclusivo);
        return ler(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> consulta = cb.createQuery(Object[].class);
            Root<Venda> venda = consulta.from(Venda.class);
            Join<Venda, UnidadeFarmacia> unidade = venda.join("unidade");
            Path<Date> data = venda.get("dataHora");
            Expression<Long> quantidade = cb.count(venda);
            consulta.multiselect(unidade.get("id"), unidade.get("nome"), quantidade)
                    .where(cb.equal(venda.get("status"), StatusVenda.CONCLUIDA),
                           cb.greaterThanOrEqualTo(data, inicio), cb.lessThan(data, fimExclusivo))
                    .groupBy(unidade.get("id"), unidade.get("nome"))
                    .orderBy(cb.asc(unidade.get("id")));
            return em.createQuery(consulta).getResultList();
        });
    }
}
