package br.edu.bd2;

import br.edu.bd2.dao.CategoriaDAO;
import br.edu.bd2.dao.ClienteDAO;
import br.edu.bd2.dao.DepartamentoDAO;
import br.edu.bd2.dao.FuncionarioDAO;
import br.edu.bd2.dao.ItemVendaDAO;
import br.edu.bd2.dao.MedicamentoDAO;
import br.edu.bd2.dao.PagamentoDAO;
import br.edu.bd2.dao.UnidadeFarmaciaDAO;
import br.edu.bd2.dao.VendaDAO;
import br.edu.bd2.modelo.CargoFuncionario;
import br.edu.bd2.modelo.Categoria;
import br.edu.bd2.modelo.Cliente;
import br.edu.bd2.modelo.Departamento;
import br.edu.bd2.modelo.FormaPagamento;
import br.edu.bd2.modelo.Funcionario;
import br.edu.bd2.modelo.ItemVenda;
import br.edu.bd2.modelo.ItemVendaId;
import br.edu.bd2.modelo.Medicamento;
import br.edu.bd2.modelo.Pagamento;
import br.edu.bd2.modelo.Pessoa;
import br.edu.bd2.modelo.StatusVenda;
import br.edu.bd2.modelo.TipoControleMedicamento;
import br.edu.bd2.modelo.UnidadeFarmacia;
import br.edu.bd2.modelo.Venda;
import br.edu.bd2.persistencia.Jpa;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static br.edu.bd2.DadosTeste.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "orm.test.postgresql", matches = "true")
class PostgresqlTest {
    private final Date dia1 = data("2026-01-10T15:00:00Z");
    private final Date dia2 = data("2026-01-11T15:00:00Z");
    private final Date dia3 = data("2026-01-12T15:00:00Z");
    private final Date dia4 = data("2026-01-13T15:00:00Z");
    private final Map<String, String> propriedadesAnteriores = new HashMap<>();
    private final String esquema = "orm_test_" + UUID.randomUUID().toString().replace("-", "");
    private Connection conexao;
    private boolean esquemaCriado;

    private Departamento medicamentos;
    private Categoria analgesicos;
    private Categoria vitaminas;
    private Cliente alana;
    private Cliente bruno;
    private UnidadeFarmacia centro;
    private UnidadeFarmacia sul;
    private Funcionario luiza;
    private Funcionario caio;
    private Medicamento dipirona;
    private Medicamento suplemento;
    private Medicamento controlado;
    private Medicamento outro;
    private Venda venda1;
    private Venda venda2;
    private Venda venda3;
    private Venda venda4;
    private Venda venda5;

    @BeforeEach
    void iniciarBancoDeTeste() throws SQLException, IOException {
        String url = variavelObrigatoria("ORM_TEST_JDBC_URL");
        String usuario = variavelObrigatoria("ORM_TEST_JDBC_USER");
        String senha = System.getenv("ORM_TEST_JDBC_PASSWORD");
        conexao = DriverManager.getConnection(url, usuario, senha);
        try (Statement sql = conexao.createStatement()) {
            sql.execute("CREATE SCHEMA " + esquema);
            esquemaCriado = true;
            conexao.setSchema(esquema);
            byte[] ddl = Files.readAllBytes(Paths.get("..", "docs", "OLTP", "diagrama_oltp.sql"));
            sql.execute(new String(ddl, StandardCharsets.UTF_8));
        }

        sobrescreverPropriedade("javax.persistence.jdbc.url",
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + esquema);
        sobrescreverPropriedade("javax.persistence.jdbc.user", usuario);
        sobrescreverPropriedade("javax.persistence.jdbc.password", senha == null ? "" : senha);
        inserirCenario();
    }

    private static String variavelObrigatoria(String nome) {
        return Objects.requireNonNull(System.getenv(nome), "Defina " + nome + " para o perfil postgresql");
    }

    private void sobrescreverPropriedade(String chave, String valor) {
        propriedadesAnteriores.put(chave, System.setProperty(chave, valor));
    }

    @AfterEach
    void encerrarBancoDeTeste() throws SQLException {
        try {
            Jpa.fechar();
        } finally {
            propriedadesAnteriores.forEach((chave, valor) -> {
                if (valor == null) {
                    System.clearProperty(chave);
                } else {
                    System.setProperty(chave, valor);
                }
            });
            if (conexao != null) {
                try (Connection banco = conexao; Statement sql = banco.createStatement()) {
                    if (esquemaCriado) {
                        sql.execute("DROP SCHEMA " + esquema + " CASCADE");
                    }
                }
            }
        }
    }

    private void inserirCenario() {
        EntityManager em = Jpa.abrirEntityManager();
        try {
            em.getTransaction().begin();
            UnidadeFarmaciaDAO unidades = new UnidadeFarmaciaDAO(em);
            centro = unidades.salvar(unidade("Centro", "00000000000001"));
            sul = unidades.salvar(unidade("Sul", "00000000000002"));
            ClienteDAO clientes = new ClienteDAO(em);
            alana = clientes.salvar(cliente("Alana", "00000000001", "F1"));
            bruno = clientes.salvar(cliente("Bruno", "00000000002", "F2"));
            clientes.salvar(cliente("Clara", "00000000003", "F3"));
            FuncionarioDAO funcionarios = new FuncionarioDAO(em);
            luiza = funcionarios.salvar(funcionario("Luiza", "00000000004", "M1", centro));
            caio = funcionarios.salvar(funcionario("Caio", "00000000005", "M2", sul));

            DepartamentoDAO departamentos = new DepartamentoDAO(em);
            medicamentos = departamentos.salvar(new Departamento("Medicamentos", null));
            Departamento cuidados = departamentos.salvar(new Departamento("Cuidados", null));
            CategoriaDAO categorias = new CategoriaDAO(em);
            analgesicos = categorias.salvar(new Categoria("Analgésicos", null, medicamentos));
            vitaminas = categorias.salvar(new Categoria("Vitaminas", null, medicamentos));
            Categoria higiene = categorias.salvar(new Categoria("Higiene", null, cuidados));

            MedicamentoDAO catalogo = new MedicamentoDAO(em);
            dipirona = catalogo.salvar(medicamento("Dipirona", "1", analgesicos, TipoControleMedicamento.ISENTO));
            suplemento = catalogo.salvar(medicamento("Suplemento", "2", vitaminas, TipoControleMedicamento.ISENTO));
            controlado = catalogo.salvar(medicamento("Controlado", "3", analgesicos, TipoControleMedicamento.CONTROLADO));
            outro = catalogo.salvar(medicamento("Outro", "4", higiene, TipoControleMedicamento.TARJA_VERMELHA));

            VendaDAO vendas = new VendaDAO(em);
            venda1 = new Venda(alana, centro, luiza, dia1);
            venda1.adicionarItem(2, suplemento, 1, valor("5.00"), valor("0"));
            venda1.adicionarItem(1, dipirona, 2, valor("10.00"), valor("1.00"));
            venda1.registrarPagamento(FormaPagamento.PIX, dia1, "TX-1").confirmar();
            vendas.salvar(venda1);
            venda2 = vendas.salvar(venda(alana, centro, luiza, dia2, dipirona, 1, "12.00", "TX-2"));
            venda3 = vendas.salvar(venda(bruno, sul, caio, dia2, suplemento, 4, "5.00", "TX-3"));
            venda4 = vendas.salvar(venda(bruno, sul, caio, dia3, controlado, 2, "7.00", "TX-4"));
            vendas.cancelar(venda4.getId());
            venda5 = vendas.salvar(venda(bruno, sul, caio, dia4, dipirona, 1, "10.00", "TX-5"));
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    private Venda venda(Cliente cliente, UnidadeFarmacia unidade, Funcionario funcionario,
                         Date dia, Medicamento medicamento, int quantidade, String preco, String numeroTransacao) {
        Venda venda = new Venda(cliente, unidade, funcionario, dia);
        venda.adicionarItem(1, medicamento, quantidade, valor(preco), valor("0"));
        venda.registrarPagamento(FormaPagamento.PIX, dia, numeroTransacao).confirmar();
        return venda;
    }

    @Test
    void mapeiaEntidadesChavesEAssociacoesNosDoisSentidos() {
        EntityManager em = Jpa.abrirEntityManager();
        try {
            assertEquals(10, em.getMetamodel().getEntities().size());
            assertTrue(em.find(Pessoa.class, alana.getId()) instanceof Cliente);
            assertTrue(em.find(Pessoa.class, luiza.getId()) instanceof Funcionario);
            Cliente cliente = new ClienteDAO(em).buscarPorCpf(alana.getCpf()).get();
            Funcionario funcionario = new FuncionarioDAO(em).buscarPorMatricula(luiza.getMatricula()).get();
            UnidadeFarmacia unidade = funcionario.getUnidade();
            assertEquals("Recife", cliente.getEndereco().getCidade());
            assertEquals("PE", unidade.getEndereco().getEstado());
            assertEquals(CargoFuncionario.FARMACEUTICO, funcionario.getCargo());
            assertEquals(alana.getId(), new ClienteDAO(em).buscarPorCartaoFidelidade("F1").get().getId());
            assertEquals(1, new FuncionarioDAO(em).listarPorUnidade(centro.getId()).size());
            assertEquals(2, new FuncionarioDAO(em).listarPorCargo(CargoFuncionario.FARMACEUTICO).size());
            assertEquals(2, new CategoriaDAO(em).listarPorDepartamento(medicamentos.getId()).size());
            assertEquals(centro.getId(), unidade.getId());
            assertTrue(unidade.getFuncionarios().contains(funcionario));
            assertEquals(2, cliente.getVendas().size());
            assertEquals(2, funcionario.getVendas().size());
            assertEquals(2, unidade.getVendas().size());

            Venda venda = em.find(Venda.class, venda1.getId());
            assertSame(cliente, venda.getCliente());
            assertSame(funcionario, venda.getFuncionario());
            assertEquals(valor("24.00"), venda.getValorTotal());
            assertEquals(2, venda.getItens().size());
            ItemVenda item = venda.getItens().get(0);
            assertEquals(Integer.valueOf(1), item.getNumeroItem());
            assertSame(venda, item.getVenda());
            assertSame(item, new ItemVendaDAO(em).buscarPorId(new ItemVendaId(venda.getId(), 1)).get());
            assertEquals(dipirona.getId(), item.getMedicamento().getId());
            assertEquals(valor("19.00"), item.getValorLiquido());
            assertEquals(2, new ItemVendaDAO(em).listarPorVenda(venda.getId()).size());
            Categoria categoria = item.getMedicamento().getCategoria();
            assertEquals(medicamentos.getId(), categoria.getDepartamento().getId());
            assertTrue(categoria.getDepartamento().getCategorias().contains(categoria));
            assertTrue(categoria.getMedicamentos().contains(item.getMedicamento()));
            assertEquals(TipoControleMedicamento.ISENTO, item.getMedicamento().getTipoControle());
            assertEquals(dipirona.getId(), new MedicamentoDAO(em).buscarPorCodigoBarras("1").get().getId());

            Pagamento pagamento = new PagamentoDAO(em).buscarPorNumeroTransacao("TX-1").get();
            assertSame(venda, pagamento.getVenda());
            assertSame(pagamento, venda.getPagamento());
            assertEquals(venda.getId(), pagamento.getId());
            assertEquals(FormaPagamento.PIX, pagamento.getFormaPagamento());
            assertTrue(pagamento.isAprovado());
            assertEquals(5, new PagamentoDAO(em).listarPorFormaPagamento(FormaPagamento.PIX).size());
        } finally {
            em.close();
        }
    }

    @Test
    void crudComContextoProprioEPrecoHistorico() {
        DepartamentoDAO dao = new DepartamentoDAO();
        Departamento departamento = dao.salvar(new Departamento("Temporário", null));
        assertTrue(dao.existe(departamento.getId()));
        assertEquals(3, dao.listarTodos().size());
        departamento.setDescricao("Descrição atualizada");
        dao.atualizar(departamento);
        assertEquals("Descrição atualizada", dao.buscarPorId(departamento.getId()).get().getDescricao());
        assertTrue(dao.excluirPorId(departamento.getId()));
        assertFalse(dao.existe(departamento.getId()));
        assertFalse(dao.excluirPorId(departamento.getId()));

        MedicamentoDAO catalogo = new MedicamentoDAO();
        Medicamento medicamento = catalogo.buscarPorId(dipirona.getId()).get();
        medicamento.setPrecoBase(valor("99.00"));
        catalogo.atualizar(medicamento);
        ItemVenda item = new ItemVendaDAO().buscarPorId(new ItemVendaId(venda1.getId(), 1)).get();
        assertEquals(valor("10.00"), item.getPrecoUnitario());
        assertEquals(valor("19.00"), item.getValorLiquido());
    }

    @Test
    void gravaVendaComAssociacoesDestacadasECancelaSemRemoverHistorico() {
        Cliente cliente = new ClienteDAO().buscarPorId(alana.getId()).get();
        Funcionario funcionario = new FuncionarioDAO().buscarPorId(luiza.getId()).get();
        UnidadeFarmacia unidade = new UnidadeFarmaciaDAO().buscarPorId(centro.getId()).get();
        Medicamento medicamento = new MedicamentoDAO().buscarPorId(dipirona.getId()).get();
        Venda criada = venda(cliente, unidade, funcionario, dia4, medicamento, 1, "7.00", null);
        VendaDAO dao = new VendaDAO();
        dao.salvar(criada);
        Venda desatualizada = dao.buscarPorId(criada.getId()).get();
        desatualizada.setObservacao("Compra no balcão");
        dao.atualizar(desatualizada);
        assertEquals("Compra no balcão", dao.buscarPorId(criada.getId()).get().getObservacao());
        assertTrue(dao.excluirPorId(criada.getId()));
        assertTrue(dao.cancelar(criada.getId()));
        assertFalse(dao.cancelar(-1L));
        assertThrows(IllegalStateException.class, () -> dao.atualizar(desatualizada));

        Venda cancelada = dao.buscarPorId(criada.getId()).get();
        assertEquals(StatusVenda.CANCELADA, cancelada.getStatus());
        assertFalse(cancelada.getPagamento().isAprovado());
        assertEquals(1, new ItemVendaDAO().listarPorVenda(criada.getId()).size());
        assertThrows(UnsupportedOperationException.class,
                () -> new ItemVendaDAO().excluirPorId(new ItemVendaId(criada.getId(), 1)));
        assertThrows(UnsupportedOperationException.class, () -> new PagamentoDAO().excluirPorId(criada.getId()));
    }

    @Test
    void impedeQueContextoCompartilhadoDesatualizadoReabraUmaVendaCancelada() {
        EntityManager em = Jpa.abrirEntityManager();
        Long id = venda2.getId();
        try {
            em.getTransaction().begin();
            VendaDAO vendas = new VendaDAO(em);
            Venda desatualizada = vendas.buscarPorId(id).get();
            assertEquals(StatusVenda.CONCLUIDA, desatualizada.getStatus());
            assertTrue(new VendaDAO().cancelar(id));
            desatualizada.setObservacao("Edição concorrente");
            assertThrows(IllegalStateException.class, () -> vendas.atualizar(desatualizada));
            assertTrue(em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        Venda cancelada = new VendaDAO().buscarPorId(id).get();
        assertEquals(StatusVenda.CANCELADA, cancelada.getStatus());
        assertFalse(cancelada.getPagamento().isAprovado());
        assertNull(cancelada.getObservacao());
    }

    @Test
    void validaVendaCompletaAntesDePersistir() {
        VendaDAO dao = new VendaDAO();
        Venda vazia = new Venda(alana, centro, luiza, dia1);
        assertThrows(IllegalStateException.class, () -> dao.salvar(vazia));
        Venda semPagamento = new Venda(alana, centro, luiza, dia1);
        semPagamento.adicionarItem(1, dipirona, 1, valor("10.00"), valor("0"));
        assertThrows(IllegalStateException.class, () -> dao.salvar(semPagamento));
        assertEquals(5, dao.listarTodos().size());
    }

    @Test
    void reverteVendaEItensSePagamentoViolarUnicidade() {
        VendaDAO dao = new VendaDAO();
        Venda repetida = venda(alana, centro, luiza, dia4, dipirona, 1, "10.00", "TX-1");
        verificarDuplicidade(() -> dao.salvar(repetida));
        assertEquals(5, dao.listarTodos().size());
        assertEquals(6, new ItemVendaDAO().listarTodos().size());
        assertEquals(5, new PagamentoDAO().listarTodos().size());
        assertEquals(2, dao.listarPorCliente(alana.getId()).size());
    }

    @Test
    void respeitaUnicidadeEBloqueiaCommitDaTransacaoCompartilhadaComFalha() {
        EntityManager em = Jpa.abrirEntityManager();
        try {
            DepartamentoDAO departamentos = new DepartamentoDAO(em);
            assertThrows(IllegalStateException.class, () -> departamentos.salvar(new Departamento("Fora", null)));
            em.getTransaction().begin();
            Departamento temporario = departamentos.salvar(new Departamento("Transação", null));
            verificarDuplicidade(() -> new FuncionarioDAO(em)
                    .salvar(funcionario("Ana", alana.getCpf(), "M3", centro)));
            assertTrue(em.isOpen());
            assertTrue(em.getTransaction().getRollbackOnly());
            em.getTransaction().rollback();
            assertFalse(new DepartamentoDAO().existe(temporario.getId()));
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }

        verificarDuplicidade(() -> new ClienteDAO().salvar(cliente("Dora", "00000000006", alana.getNumeroCartaoFidelidade())));
        verificarDuplicidade(() -> new FuncionarioDAO()
                .salvar(funcionario("Eva", "00000000007", luiza.getMatricula(), centro)));
        verificarDuplicidade(() -> new UnidadeFarmaciaDAO().salvar(unidade("Repetida", centro.getCnpj())));
        verificarDuplicidade(() -> new CategoriaDAO().salvar(new Categoria(analgesicos.getNome(), null, medicamentos)));
        verificarDuplicidade(() -> new MedicamentoDAO()
                .salvar(medicamento("Repetido", dipirona.getCodigoBarras(), analgesicos, TipoControleMedicamento.ISENTO)));
    }

    private static void verificarDuplicidade(org.junit.jupiter.api.function.Executable escrita) {
        Throwable causa = assertThrows(PersistenceException.class, escrita);
        while (causa != null && !(causa instanceof SQLException)) {
            causa = causa.getCause();
        }
        assertNotNull(causa, "A exceção JPA deve preservar o erro SQL");
        assertEquals("23505", ((SQLException) causa).getSQLState());
    }

    @Test
    void executaAsDezConsultasJpqlInclusiveNamedQueries() {
        VendaDAO vendas = new VendaDAO();
        assertEquals(Arrays.asList(venda2.getId(), venda1.getId()), ids(vendas.listarPorCliente(alana.getId())));
        assertTrue(vendas.listarPorCliente(-1L).isEmpty());
        List<Venda> comItens = vendas.listarComItensPorCliente(alana.getId());
        assertEquals(Arrays.asList(venda2.getId(), venda1.getId()), ids(comItens));
        assertEquals(2, comItens.get(1).getItens().size());
        assertEquals(Integer.valueOf(1), comItens.get(1).getItens().get(0).getNumeroItem());

        List<Object[]> porCliente = vendas.totalPorCliente(valor("31.00"));
        assertEquals(1, porCliente.size());
        assertArrayEquals(new Object[]{alana.getId(), "Alana", valor("36.00")}, porCliente.get(0));
        List<Object[]> faturamento = vendas.faturamentoPorUnidade(dia1, dia4);
        assertEquals(2, faturamento.size());
        assertArrayEquals(new Object[]{centro.getId(), "Centro", valor("36.00")}, faturamento.get(0));
        assertArrayEquals(new Object[]{sul.getId(), "Sul", valor("20.00")}, faturamento.get(1));

        MedicamentoDAO catalogo = new MedicamentoDAO();
        List<Object[]> ranking = catalogo.listarMaisVendidos();
        assertEquals(2, ranking.size());
        assertArrayEquals(new Object[]{suplemento.getId(), "Suplemento", 5L}, ranking.get(0));
        assertArrayEquals(new Object[]{dipirona.getId(), "Dipirona", 4L}, ranking.get(1));
        assertEquals(Arrays.asList(venda4.getId()),
                ids(vendas.listarPorStatusEPeriodo(StatusVenda.CANCELADA, dia1, dia4)));
        assertEquals(Arrays.asList(controlado.getId(), outro.getId()), catalogo.listarNuncaVendidos()
                .stream().map(Medicamento::getId).collect(Collectors.toList()));

        assertEquals(Arrays.asList(venda5.getId(), venda4.getId()), ids(vendas.listarPaginadas(0, 2)));
        assertEquals(Arrays.asList(venda3.getId(), venda2.getId()), ids(vendas.listarPaginadas(2, 2)));
        assertTrue(vendas.listarPaginadas(5, 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> vendas.listarPaginadas(0, 0));
        assertEquals(Arrays.asList(alana.getId(), bruno.getId()), new ClienteDAO().listarComHistorico()
                .stream().map(Cliente::getId).collect(Collectors.toList()));

        List<Object[]> porCategoria = new ItemVendaDAO().valorPorCategoria();
        assertEquals(2, porCategoria.size());
        assertArrayEquals(new Object[]{analgesicos.getId(), "Analgésicos", valor("41.00")}, porCategoria.get(0));
        assertArrayEquals(new Object[]{vitaminas.getId(), "Vitaminas", valor("25.00")}, porCategoria.get(1));
    }

    @Test
    void executaAsTresConsultasCriteriaComFiltrosCombinadosEOpcionais() {
        VendaDAO vendas = new VendaDAO();
        assertEquals(5, vendas.buscarComFiltros(null, null, null, null, null, null).size());
        assertEquals(Arrays.asList(venda2.getId()), ids(vendas.buscarComFiltros(alana.getId(), centro.getId(),
                luiza.getId(), StatusVenda.CONCLUIDA, dia2, dia4)));
        assertEquals(Arrays.asList(venda4.getId()), ids(vendas.buscarComFiltros(null, null, null,
                StatusVenda.CANCELADA, null, dia4)));
        assertTrue(vendas.buscarComFiltros(alana.getId(), sul.getId(), null, null, dia1, null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> vendas.buscarComFiltros(null, null, null, null, dia4, dia1));

        MedicamentoDAO catalogo = new MedicamentoDAO();
        assertEquals(4, catalogo.buscarComFiltros(null, null, null).size());
        assertEquals(3, catalogo.buscarComFiltros(null, medicamentos.getId(), null).size());
        List<Medicamento> filtrados = catalogo.buscarComFiltros(analgesicos.getId(), medicamentos.getId(),
                TipoControleMedicamento.CONTROLADO);
        assertEquals(1, filtrados.size());
        assertEquals(controlado.getId(), filtrados.get(0).getId());
        assertTrue(catalogo.buscarComFiltros(vitaminas.getId(), null, TipoControleMedicamento.CONTROLADO).isEmpty());

        List<Object[]> porUnidade = vendas.contarConcluidasPorUnidade(dia1, dia4);
        assertEquals(2, porUnidade.size());
        assertArrayEquals(new Object[]{centro.getId(), "Centro", 2L}, porUnidade.get(0));
        assertArrayEquals(new Object[]{sul.getId(), "Sul", 1L}, porUnidade.get(1));
        assertTrue(vendas.contarConcluidasPorUnidade(dia3, dia4).isEmpty());
    }

    private static List<Long> ids(List<Venda> vendas) {
        return vendas.stream().map(Venda::getId).collect(Collectors.toList());
    }
}
