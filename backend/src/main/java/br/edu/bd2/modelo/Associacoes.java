package br.edu.bd2.modelo;

import javax.persistence.Persistence;

final class Associacoes {
    private Associacoes() {
    }

    // Coleções inversas ainda não lidas serão carregadas pela chave estrangeira.
    static boolean colecaoCarregada(Object entidade, String atributo) {
        return entidade != null && Persistence.getPersistenceUtil().isLoaded(entidade, atributo);
    }
}
