package br.edu.bd2.dao;

import br.edu.bd2.modelo.FormaPagamento;
import br.edu.bd2.modelo.Pagamento;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;

public class PagamentoDAO extends GenericDAO<Pagamento, Long> {
    public PagamentoDAO() {
        super(Pagamento.class);
    }

    public PagamentoDAO(EntityManager em) {
        super(Pagamento.class, em);
    }

    @Override
    protected void validarEscrita() {
        throw new UnsupportedOperationException("Pagamentos são gravados com a venda pelo VendaDAO");
    }

    public Optional<Pagamento> buscarPorNumeroTransacao(String numeroTransacao) {
        return ler(em -> em.createQuery(
                "SELECT p FROM Pagamento p WHERE p.numeroTransacao = :numero", Pagamento.class)
                .setParameter("numero", numeroTransacao)
                .getResultList().stream().findFirst());
    }

    public List<Pagamento> listarPorFormaPagamento(FormaPagamento formaPagamento) {
        return ler(em -> em.createQuery(
                "SELECT p FROM Pagamento p WHERE p.formaPagamento = :forma ORDER BY p.dataHora DESC, p.id DESC",
                Pagamento.class)
                .setParameter("forma", formaPagamento)
                .getResultList());
    }
}
