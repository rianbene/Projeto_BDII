package br.edu.bd2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FarmaciaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmaciaApplication.class, args);
    }

// O BeanFactoryPostProcessor roda antes do DataSource e do Hibernate inicializarem, em resumo ele é executado antes do Spring Boot tentar se conectar ao banco de dados.
    @Bean
    public static BeanFactoryPostProcessor databaseInitializer(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password}") String password) {
        return beanFactory -> {
            String dbName = url.substring(url.lastIndexOf("/") + 1);
            String adminUrl = url.substring(0, url.lastIndexOf("/") + 1) + "postgres";

            try (Connection conn = DriverManager.getConnection(adminUrl, user, password);
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery(
                        "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");

                if (!rs.next()) {
                    stmt.executeUpdate("CREATE DATABASE " + dbName);
                    System.out.println("[SPRING INIT] Banco de dados '" + dbName + "' criado automaticamente!");
                }
            } catch (Exception e) {
                System.err.println("[SPRING INIT] Aviso ao verificar/criar banco: " + e.getMessage());
            }
        };
    }
}