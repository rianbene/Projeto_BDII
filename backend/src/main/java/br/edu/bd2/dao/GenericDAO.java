package br.edu.bd2.dao;

import br.edu.bd2.persistencia.Jpa;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaQuery;

public class GenericDAO<T, ID> {
    private final Class<T> entidade;
    private final EntityManager compartilhado;

    public GenericDAO(Class<T> entidade) {
        this(entidade, null);
    }

    /**
     * Um EntityManager fornecido pelo chamador permanece aberto.
     * O chamador inicia e encerra a transação compartilhada.
     */
    public GenericDAO(Class<T> entidade, EntityManager compartilhado) {
        this.entidade = Objects.requireNonNull(entidade, "Classe da entidade obrigatória");
        this.compartilhado = compartilhado;
    }

    public T salvar(T registro) {
        return escrever("persistir", em -> {
            em.persist(Objects.requireNonNull(registro, "Registro obrigatório"));
            return registro;
        });
    }

    public Optional<T> buscarPorId(ID id) {
        return ler(em -> Optional.ofNullable(em.find(entidade, id)));
    }

    public List<T> listarTodos() {
        return ler(em -> {
            CriteriaQuery<T> consulta = em.getCriteriaBuilder().createQuery(entidade);
            consulta.select(consulta.from(entidade));
            return em.createQuery(consulta).getResultList();
        });
    }

    public T atualizar(T registro) {
        Objects.requireNonNull(registro, "Registro obrigatório");
        return escrever("atualizar", em -> mesclar(em, registro));
    }

    public boolean excluirPorId(ID id) {
        return escrever("excluir", em -> excluir(em, id));
    }

    public boolean existe(ID id) {
        return buscarPorId(id).isPresent();
    }

    protected T mesclar(EntityManager em, T registro) {
        return em.merge(registro);
    }

    protected boolean excluir(EntityManager em, ID id) {
        T registro = em.find(entidade, id);
        if (registro == null) {
            return false;
        }
        em.remove(registro);
        return true;
    }

    protected void validarEscrita() {
    }

    protected final <R> R ler(Function<EntityManager, R> consulta) {
        return comEntityManager(consulta);
    }

    private <R> R escrever(String acao, Function<EntityManager, R> operacao) {
        validarEscrita();
        return comEntityManager(em -> {
            boolean transacaoPropria = compartilhado == null;
            EntityTransaction transacao = em.getTransaction();
            if (!transacaoPropria && !transacao.isActive()) {
                throw new IllegalStateException("Inicie a transação do EntityManager compartilhado");
            }
            try {
                if (transacaoPropria) {
                    transacao.begin();
                }
                R resultado = operacao.apply(em);
                if (transacaoPropria) {
                    transacao.commit();
                }
                return resultado;
            } catch (RuntimeException | Error falha) {
                encerrarComFalha(transacao, transacaoPropria, falha);
                if (falha instanceof PersistenceException) {
                    throw new PersistenceException(
                            "Falha ao " + acao + " " + entidade.getSimpleName(), falha);
                }
                throw falha;
            }
        });
    }

    private void encerrarComFalha(EntityTransaction transacao, boolean transacaoPropria, Throwable falha) {
        try {
            if (transacao.isActive()) {
                if (transacaoPropria) {
                    transacao.rollback();
                } else {
                    transacao.setRollbackOnly();
                }
            }
        } catch (RuntimeException falhaNaTransacao) {
            falha.addSuppressed(falhaNaTransacao);
        }
    }

    private <R> R comEntityManager(Function<EntityManager, R> operacao) {
        if (compartilhado != null) {
            return operacao.apply(compartilhado);
        }
        EntityManager em = Jpa.abrirEntityManager();
        try {
            return operacao.apply(em);
        } finally {
            em.close();
        }
    }
}
