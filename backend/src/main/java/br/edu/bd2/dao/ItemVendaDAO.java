package br.edu.bd2.dao;

import br.edu.bd2.modelo.ItemVenda;
import br.edu.bd2.modelo.ItemVendaId;
import br.edu.bd2.modelo.StatusVenda;
import java.util.List;
import javax.persistence.EntityManager;

public class ItemVendaDAO extends GenericDAO<ItemVenda, ItemVendaId> {
    public ItemVendaDAO() {
        super(ItemVenda.class);
    }

    public ItemVendaDAO(EntityManager em) {
        super(ItemVenda.class, em);
    }

    @Override
    protected void validarEscrita() {
        throw new UnsupportedOperationException("Itens são gravados com a venda pelo VendaDAO");
    }

    public List<ItemVenda> listarPorVenda(Long vendaId) {
        return ler(em -> em.createQuery(
                "SELECT i FROM ItemVenda i WHERE i.id.vendaId = :id ORDER BY i.id.numeroItem", ItemVenda.class)
                .setParameter("id", vendaId)
                .getResultList());
    }

    /** Cada linha contém Long categoriaId, String nome e BigDecimal valor líquido total. */
    public List<Object[]> valorPorCategoria() {
        return ler(em -> em.createQuery(
                "SELECT c.id, c.nome, SUM(i.valorLiquido) FROM ItemVenda i JOIN i.medicamento m "
                    + "JOIN m.categoria c WHERE i.venda.status = :status "
                    + "GROUP BY c.id, c.nome ORDER BY c.id", Object[].class)
                .setParameter("status", StatusVenda.CONCLUIDA)
                .getResultList());
    }
}
