package br.edu.bd2.app;

import java.util.List;
import java.util.Scanner;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import br.edu.bd2.dao.DepartamentoDAO;
import br.edu.bd2.dao.UnidadeFarmaciaDAO;
import br.edu.bd2.modelo.Departamento;
import br.edu.bd2.modelo.Endereco;
import br.edu.bd2.modelo.UnidadeFarmacia;
import br.edu.bd2.service.CargaDadosService;

@Component
public class MenuConsole implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CargaDadosService cargaDadosService;

    private Scanner sc = new Scanner(System.in);
    private DepartamentoDAO departamentoDAO;
    private UnidadeFarmaciaDAO unidadeDAO;

    @PostConstruct
    public void init() {
        this.departamentoDAO = new DepartamentoDAO(em);
        this.unidadeDAO = new UnidadeFarmaciaDAO(em);
    }

    @Override
    public void run(String... args) throws Exception {
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=============================================");
            System.out.println("   SISTEMA OLTP - REDE DE FARMÁCIAS (BD2)");
            System.out.println("=============================================");
            System.out.println("1. CRUD Departamentos");
            System.out.println("2. CRUD Unidades da Farmácia (Lojas)");
            System.out.println("3. Executar Carga de Dados (2.000 vendas)");
            System.out.println("4. Executar Consultas JPQL e Criteria API");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            try {
                opcao = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Erro: Digite apenas números!");
                continue;
            }

            switch (opcao) {
                case 1:
                    crudDepartamento();
                    break;
                case 2:
                    crudUnidadeFarmacia();
                    break;
                case 3:
                    cargaDadosService.executarCarga();
                    break;
                case 4:
                    System.out.println("\n[VALIDAÇÃO] Executando consultas JPQL e Criteria...");
                    break;
                case 0:
                    System.out.println("Encerrando aplicação...");
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        }
        sc.close();
    }

    @Transactional
    public void crudDepartamento() {
        System.out.println("\n--- CRUD DEPARTAMENTO ---");
        System.out.println("1. Cadastrar | 2. Listar | 3. Atualizar | 4. Remover");
        System.out.print("Escolha uma operação: ");
        int op = Integer.parseInt(sc.nextLine());

        if (op == 1) {
            System.out.print("Nome do Departamento: ");
            String nome = sc.nextLine();
            System.out.print("Descrição do Departamento: ");
            String descricao = sc.nextLine();

            departamentoDAO.salvar(new Departamento(nome, descricao));
            System.out.println("Departamento cadastrado com sucesso!");

        } else if (op == 2) {
            List<Departamento> lista = departamentoDAO.listarTodos();
            System.out.println("\n--- Lista de Departamentos ---");
            for (Departamento d : lista) {
                System.out.println("ID: " + d.getId() + " | Nome: " + d.getNome() + " | Desc: " + d.getDescricao());
            }

        } else if (op == 3) {
            System.out.print("ID do Departamento para editar: ");
            Long id = Long.parseLong(sc.nextLine());
            Departamento d = departamentoDAO.buscarPorId(id).orElse(null);
            if (d != null) {
                System.out.print("Novo Nome: ");
                d.setNome(sc.nextLine());
                System.out.print("Nova Descrição: ");
                d.setDescricao(sc.nextLine());
                departamentoDAO.atualizar(d);
                System.out.println("Departamento atualizado!");
            } else {
                System.out.println("Departamento não encontrado.");
            }

        } else if (op == 4) {
            System.out.print("ID do Departamento para remover: ");
            Long id = Long.parseLong(sc.nextLine());
            departamentoDAO.excluirPorId(id);
            System.out.println("Departamento removido!");
        }
    }

    @Transactional
    public void crudUnidadeFarmacia() {
        System.out.println("\n--- CRUD UNIDADE FARMÁCIA ---");
        System.out.println("1. Cadastrar | 2. Listar | 3. Atualizar | 4. Remover");
        System.out.print("Escolha uma operação: ");
        int op = Integer.parseInt(sc.nextLine());

        if (op == 1) {
            System.out.print("Nome da Unidade: ");
            String nome = sc.nextLine();
            System.out.print("CNPJ (14 dígitos): ");
            String cnpj = sc.nextLine();
            System.out.print("Telefone: ");
            String telefone = sc.nextLine();

            System.out.print("Rua: ");
            String rua = sc.nextLine();
            System.out.print("Número: ");
            String numero = sc.nextLine();
            System.out.print("Complemento (opcional): ");
            String complemento = sc.nextLine();
            System.out.print("Bairro: ");
            String bairro = sc.nextLine();
            System.out.print("Cidade: ");
            String cidade = sc.nextLine();
            System.out.print("Estado (UF - 2 letras): ");
            String estado = sc.nextLine();
            System.out.print("CEP (8 dígitos): ");
            String cep = sc.nextLine();

            Endereco end = new Endereco(rua, numero, complemento, bairro, cidade, estado, cep);
            unidadeDAO.salvar(new UnidadeFarmacia(nome, cnpj, telefone, end));
            System.out.println("Unidade Farmácia cadastrada com sucesso!");

        } else if (op == 2) {
            List<UnidadeFarmacia> lista = unidadeDAO.listarTodos();
            System.out.println("\n--- Lista de Unidades ---");
            for (UnidadeFarmacia u : lista) {
                System.out.println("ID: " + u.getId() + " | Loja: " + u.getNome() + " | CNPJ: " + u.getCnpj() + " | Tel: " + u.getTelefone());
            }

        } else if (op == 3) {
            System.out.print("ID da Unidade para editar: ");
            Long id = Long.parseLong(sc.nextLine());
            UnidadeFarmacia u = unidadeDAO.buscarPorId(id).orElse(null);
            if (u != null) {
                System.out.print("Novo Nome da Loja: ");
                u.setNome(sc.nextLine());
                unidadeDAO.atualizar(u);
                System.out.println("Unidade atualizada!");
            } else {
                System.out.println("Unidade não encontrada.");
            }

        } else if (op == 4) {
            System.out.print("ID da Unidade para remover: ");
            Long id = Long.parseLong(sc.nextLine());
            unidadeDAO.excluirPorId(id);
            System.out.println("Unidade removida!");
        }
    }
}