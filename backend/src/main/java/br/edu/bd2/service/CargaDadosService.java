package br.edu.bd2.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.bd2.modelo.CargoFuncionario;
import br.edu.bd2.modelo.Categoria;
import br.edu.bd2.modelo.Cliente;
import br.edu.bd2.modelo.Departamento;
import br.edu.bd2.modelo.Endereco;
import br.edu.bd2.modelo.FormaPagamento;
import br.edu.bd2.modelo.Funcionario;
import br.edu.bd2.modelo.Medicamento;
import br.edu.bd2.modelo.Pagamento;
import br.edu.bd2.modelo.TipoControleMedicamento;
import br.edu.bd2.modelo.UnidadeFarmacia;
import br.edu.bd2.modelo.Venda;

@Service
public class CargaDadosService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void executarCarga() {
        Long qtdVendasExistentes = em.createQuery("SELECT COUNT(v) FROM Venda v", Long.class).getSingleResult();
        if (qtdVendasExistentes > 0) {
            System.out.println("[ETL] O banco já possui " + qtdVendasExistentes + " vendas cadastradas. Carga ignorada para evitar duplicidade.");
            return;
        }

        System.out.println("[ETL] Iniciando carga de dados simulados no PostgreSQL...");
        Random random = new Random(42);

        // 1. DEPARTAMENTOS (5)
        String[] nomesDept = {
            "Medicamentos", "Higiene Pessoal", "Dermocosméticos", "Infantil e Maternidade", "Suplementos e Vitaminas"
        };
        List<Departamento> departamentos = new ArrayList<>();
        for (String nome : nomesDept) {
            Departamento d = new Departamento(nome, "Departamento de " + nome);
            em.persist(d);
            departamentos.add(d);
        }

        // 2. CATEGORIAS (15 - 3 por departamento)
        List<Categoria> categorias = new ArrayList<>();
        String[] sufixosCat = {"Linha Geral", "Linha Premium", "Uso Contínuo"};
        for (Departamento d : departamentos) {
            for (String suf : sufixosCat) {
                Categoria c = new Categoria(d.getNome() + " - " + suf, "Categoria " + suf + " de " + d.getNome(), d);
                em.persist(c);
                categorias.add(c);
            }
        }

        // 3. UNIDADES DA FARMÁCIA (5)
        String[] bairros = {"Pituba", "Barra", "Ipitanga", "Brotas", "Rio Vermelho"};
        List<UnidadeFarmacia> unidades = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Endereco end = new Endereco(
                "Av. Principal " + bairros[i],
                String.valueOf(100 + i * 10),
                "Loja " + (i + 1),
                bairros[i],
                i == 2 ? "Lauro de Freitas" : "Salvador",
                "BA",
                String.format("41%06d", i + 1)
            );
            UnidadeFarmacia u = new UnidadeFarmacia(
                "Farmácia Rede Saúde - " + bairros[i],
                String.format("123456780001%02d", i + 1),
                String.format("71333300%02d", i + 1),
                end
            );
            em.persist(u);
            unidades.add(u);
        }

        // 4. MEDICAMENTOS (100)
        String[] principios = {"Dipirona", "Paracetamol", "Ibuprofeno", "Amoxicilina", "Losartana", "Omeprazol", "Vitamina C", "Loratadina", "Azitromicina", "Simeticona"};
        String[] labs = {"EMS", "Medley", "Eurofarma", "Neo Química", "Ache", "Bayer"};
        String[] apresentacoes = {"Caixa 20 comprimidos", "Frasco 100ml", "Caixa 30 cápsulas", "Bisnaga 50g"};
        TipoControleMedicamento[] tiposControle = TipoControleMedicamento.values();

        List<Medicamento> medicamentos = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String principio = principios[i % principios.length];
            Categoria cat = categorias.get(i % categorias.size());
            BigDecimal preco = BigDecimal.valueOf(10 + random.nextInt(140) + (random.nextInt(100) / 100.0));
            int estoque = 50 + random.nextInt(300);

            Medicamento m = new Medicamento(
                principio + " " + (100 * ((i % 5) + 1)) + "mg #" + i,
                principio,
                String.format("7891000%06d", i),
                labs[i % labs.length],
                apresentacoes[i % apresentacoes.length],
                preco,
                estoque,
                cat,
                tiposControle[i % tiposControle.length]
            );
            em.persist(m);
            medicamentos.add(m);
        }

        // 5. FUNCIONÁRIOS (25 - 5 por unidade)
        CargoFuncionario[] cargos = CargoFuncionario.values();
        List<Funcionario> funcionarios = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            UnidadeFarmacia unidade = unidades.get((i - 1) % unidades.size());
            Endereco endFunc = new Endereco(
                "Rua dos Funcionários " + i,
                String.valueOf(i),
                null,
                "Centro",
                "Salvador",
                "BA",
                "40010000"
            );
            Funcionario f = new Funcionario(
                "Funcionário " + i,
                String.format("10000000%03d", i),
                gerarDataAleatoria(random, 1980, 2002),
                String.format("7198888%04d", i),
                "func" + i + "@redesaudefarmacia.com.br",
                endFunc,
                String.format("MAT-%04d", i),
                gerarDataAleatoria(random, 2020, 2025),
                BigDecimal.valueOf(2200 + (i % 3) * 1200),
                cargos[i % cargos.length],
                unidade
            );
            em.persist(f);
            funcionarios.add(f);
        }

        List<Cliente> clientes = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            Endereco endCli = new Endereco(
                "Rua do Cliente " + i,
                String.valueOf(10 + i),
                i % 2 == 0 ? "Apto " + i : null,
                bairros[i % bairros.length],
                "Salvador",
                "BA",
                "41800000"
            );
            Cliente c = new Cliente(
                "Cliente Simulado " + i,
                String.format("20000000%03d", i),
                gerarDataAleatoria(random, 1960, 2006),
                String.format("7199999%04d", i),
                "cliente" + i + "@email.com",
                endCli,
                String.format("FID-%06d", i),
                gerarDataAleatoria(random, 2022, 2026)
            );
            em.persist(c);
            clientes.add(c);
        }

        em.flush();
        System.out.println("[ETL] Cadastros base concluídos. Gerando 2.000 vendas...");

        FormaPagamento[] formasPagamento = FormaPagamento.values();
        for (int i = 1; i <= 2000; i++) {
            Cliente cliente = clientes.get(random.nextInt(clientes.size()));
            UnidadeFarmacia unidade = unidades.get(random.nextInt(unidades.size()));
            Funcionario funcionario = funcionarios.get(random.nextInt(funcionarios.size()));
            Date dataVenda = gerarDataAleatoria(random, 2025, 2026);

            Venda venda = new Venda(cliente, unidade, funcionario, dataVenda);
            if (i % 10 == 0) {
                venda.setObservacao("Venda gerada via carga ETL #" + i);
            }

            int qtdItens = 1 + random.nextInt(4); // 1 a 4 itens por venda
            for (int numItem = 1; numItem <= qtdItens; numItem++) {
                Medicamento med = medicamentos.get(random.nextInt(medicamentos.size()));
                int qtd = 1 + random.nextInt(3);
                BigDecimal desconto = (numItem == 1 && i % 5 == 0)
                        ? BigDecimal.valueOf(2.00)
                        : BigDecimal.ZERO;

                venda.adicionarItem(numItem, med, qtd, med.getPrecoBase(), desconto);
            }

            FormaPagamento forma = formasPagamento[random.nextInt(formasPagamento.length)];
            Pagamento pag = venda.registrarPagamento(forma, dataVenda, "TXN-" + UUID.randomUUID().toString().substring(0, 18));

            if (i % 25 == 0) {
                venda.cancelar(); // 4% de vendas canceladas para enriquecer consultas BI
            } else {
                pag.confirmar();
            }

            em.persist(venda);

            if (i % 200 == 0) {
                em.flush();
                em.clear();
                clientes = em.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList();
                unidades = em.createQuery("SELECT u FROM UnidadeFarmacia u", UnidadeFarmacia.class).getResultList();
                funcionarios = em.createQuery("SELECT f FROM Funcionario f", Funcionario.class).getResultList();
                medicamentos = em.createQuery("SELECT m FROM Medicamento m", Medicamento.class).getResultList();
                System.out.println("[ETL] " + i + " / 2.000 vendas persistidas...");
            }
        }

        em.flush();
        System.out.println("[ETL] Carga concluída com sucesso! 2.000 vendas inseridas no banco.");
    }

    private Date gerarDataAleatoria(Random random, int anoInicio, int anoFim) {
        Calendar cal = Calendar.getInstance();
        int ano = anoInicio + random.nextInt((anoFim - anoInicio) + 1);
        int diaDoAno = 1 + random.nextInt(365);
        cal.set(Calendar.YEAR, ano);
        cal.set(Calendar.DAY_OF_YEAR, diaDoAno);
        cal.set(Calendar.HOUR_OF_DAY, 8 + random.nextInt(13));
        cal.set(Calendar.MINUTE, random.nextInt(60));
        cal.set(Calendar.SECOND, random.nextInt(60));
        return cal.getTime();
    }
}