package br.edu.bd2.persistencia;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public final class Jpa {
    private static EntityManagerFactory fabrica;

    private Jpa() {
    }

    public static synchronized EntityManager abrirEntityManager() {
        if (fabrica == null) {
            Map<String, Object> propriedades = new HashMap<>();
            configurar(propriedades, "javax.persistence.jdbc.url", "VENDAS_JDBC_URL");
            configurar(propriedades, "javax.persistence.jdbc.user", "VENDAS_JDBC_USER");
            configurar(propriedades, "javax.persistence.jdbc.password", "VENDAS_JDBC_PASSWORD");
            configurar(propriedades, "hibernate.show_sql", "VENDAS_SHOW_SQL");
            fabrica = Persistence.createEntityManagerFactory("vendasPU", propriedades);
        }
        return fabrica.createEntityManager();
    }

    private static void configurar(Map<String, Object> propriedades, String chave, String variavel) {
        String valor = System.getProperty(chave, System.getenv(variavel));
        if (valor != null) {
            propriedades.put(chave, valor);
        }
    }

    /** Chame ao encerrar a aplicação, depois de fechar seus EntityManagers. */
    public static synchronized void fechar() {
        if (fabrica != null) {
            fabrica.close();
            fabrica = null;
        }
    }
}
