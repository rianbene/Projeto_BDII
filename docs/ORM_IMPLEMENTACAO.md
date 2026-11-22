# Camada ORM da Rede de Farmácias

## Diagnóstico e escopo

A cópia inicial continha o UML, o DDL PostgreSQL e um arquivo vazio em `backend/`. O diretório e seu pai estavam sem metadados Git. [UML](UML/diagrama_classes.md) · [DDL PostgreSQL](OLTP/diagrama_oltp.sql)

| Requisito | Estado inicial | Implementação |
| --- | --- | --- |
| Estrutura Java e integração | Ausente | Módulo JAR Maven em `backend/`, no pacote `br.edu.bd2` |
| Domínio e esquema | Modelados em UML e SQL | Dez entidades JPA mapeadas às tabelas existentes |
| Persistência | Ausente | `persistence.xml`, unidade `vendasPU` e fábrica em `Jpa` |
| DAOs e transações | Ausentes | `GenericDAO<T, ID>` e nove DAOs de domínio |
| JPQL e Criteria | Ausentes | Catálogos de dez consultas JPQL e três Criteria |
| Testes e documentação ORM | Ausentes | Testes unitários e de integração com PostgreSQL e este guia |

As orientações acadêmicas determinam Java 8, JPA 2.1 e Hibernate ORM 5.2.10.Final. O Anexo A exemplifica a configuração JDBC. O módulo adota pgJDBC 42.7.14, [compatível com Java 8](https://jdbc.postgresql.org/download/), e valida o esquema na inicialização. O UML define as regras de negócio. O DDL da equipe define os nomes, as chaves e os tipos físicos adotados pelo Java.

## Pacotes e mapeamentos

- `br.edu.bd2.modelo` contém as entidades, os enums, `Endereco` e a chave composta de item.
- `br.edu.bd2.dao` contém o CRUD, as consultas e os limites da transação.
- `br.edu.bd2.persistencia` fornece o ciclo de vida da fábrica e dos contextos JPA.

| Entidade | Tabela e identidade | Relacionamentos e proprietários |
| --- | --- | --- |
| `Pessoa` | `pessoa.id`, `IDENTITY` | Raiz abstrata com `@Inheritance(JOINED)` e `@Embedded` |
| `Cliente` | `cliente.id`, PK e FK para `pessoa` | Vendas por `@OneToMany(mappedBy = "cliente")` |
| `Funcionario` | `funcionario.id`, PK e FK para `pessoa` | Possui `unidade_id`. Vendas por `mappedBy = "funcionario"` |
| `UnidadeFarmacia` | `unidade_farmacia.id`, `IDENTITY` | Endereço incorporado. Funcionários e vendas por `mappedBy = "unidade"` |
| `Departamento` | `departamento.id`, `IDENTITY` | Categorias por `mappedBy = "departamento"` |
| `Categoria` | `categoria.id`, `IDENTITY` | Possui `departamento_id`. Medicamentos por `mappedBy = "categoria"` |
| `Medicamento` | `medicamento.id`, `IDENTITY` | Possui `categoria_id` |
| `Venda` | `venda.id`, `IDENTITY` | Possui `cliente_id`, `funcionario_id` e `unidade_id`. Itens e pagamento por `mappedBy = "venda"` |
| `ItemVenda` | `item_venda(venda_id, numero_item)`, `@EmbeddedId` | Possui `venda_id` com `@MapsId("vendaId")` e `medicamento_id` |
| `Pagamento` | `pagamento.venda_id`, PK e FK única | Proprietário do `@OneToOne` com `Venda`, cuja identidade reutiliza com `@MapsId` |

`JOINED` permite consultar `Pessoa` polimorficamente e dá à hierarquia uma única origem de identidade. Os dados comuns, inclusive o CPF único, ficam em `pessoa`. Cartão de fidelidade e matrícula ficam nas respectivas tabelas de subtipo. As colunas de `Endereco` já têm os mesmos nomes em `pessoa` e `unidade_farmacia`, por isso ambas reutilizam o mesmo `@Embeddable`.

`CargoFuncionario`, `FormaPagamento`, `StatusVenda` e `TipoControleMedicamento` usam `@Enumerated(STRING)`, em conformidade com os valores de texto do DDL. Nascimento e admissão usam `@Temporal(DATE)`. Cadastro, venda e pagamento usam `@Temporal(TIMESTAMP)`. As colunas são `TIMESTAMP` sem fuso. Use o mesmo fuso no processo Java e na carga de datas da equipe para interpretar os limites dos relatórios.

As FKs obrigatórias usam `optional = false` e `nullable = false`. A unidade, o cliente, o funcionário e os registros do catálogo têm ciclos de vida próprios. Os mapeamentos para eles usam `LAZY` e as coleções inversas seguem o padrão `LAZY`. Os métodos de vínculo sincronizam as coleções já carregadas. A FK do lado proprietário determina o conteúdo das coleções quando carregadas do banco.

No lado inverso de `Venda.pagamento`, a anotação usa a opcionalidade padrão. Com `optional = false` nesse lado, o Hibernate 5.2.10 agenda o filho antes da venda, o que impede a geração da chave compartilhada. A obrigatoriedade é aplicada por `@PrePersist` na venda e por `optional = false` e pela PK/FK no lado proprietário de `Pagamento`.

A venda propaga operações a seus itens e pagamento por `CascadeType.ALL` e `orphanRemoval`. Esses registros dependem da venda para existir. Departamento e categoria têm vínculo obrigatório. A FK do DDL exige transferência ou remoção explícita das categorias antes de excluir o departamento, e o mesmo vale para medicamentos da categoria. O lado inverso de pagamento em `Venda` é `EAGER` por ser um único componente obrigatório da venda e porque a detecção de um filho em `@OneToOne` inverso pode exigir consulta adicional no Hibernate 5.2 sem instrumentação. O carregamento de itens em lote é explícito em JPQL-02. [Guia Hibernate ORM 5.2](https://docs.hibernate.org/orm/5.2/userguide/html_single/#associations-one-to-one-bidirectional-lazy)

## Registro de venda e integridade

- `Venda → ItemVenda → Medicamento` representa a associação lógica muitos-para-muitos com quantidade, preço praticado, desconto e líquido por linha. A PK composta autoriza o mesmo número em vendas diferentes e garante uma única linha por número dentro de cada venda.
- O modelo e o SQL definem os campos financeiros. A interpretação monetária adotada para `desconto` é um abatimento absoluto por linha, em reais, coerente com as colunas `NUMERIC(12,2)`. O líquido é `quantidade × preço unitário − desconto`. Valores negativos, descontos acima do bruto e valores com mais de dois decimais significativos são rejeitados. O preço do item é definido no registro da venda.
- A sequência é adicionar os itens, registrar o pagamento pelo total da venda e chamar `VendaDAO.salvar()`. `@PrePersist` exige itens e pagamento mesmo quando a aplicação usa diretamente `EntityManager.persist`. `confirmar()` marca o pagamento como aprovado. `VendaDAO.cancelar(id)` ou `excluirPorId(id)` preserva o registro e altera o status e a aprovação em uma transação.
- CPF, e-mail, cartão, matrícula, CNPJ, código de barras e número de transação têm unicidade no PostgreSQL. O número de transação é opcional e valores em branco são tratados como ausentes.
- Os DAOs de itens e pagamentos usam as leituras herdadas. A escrita é feita com a venda para preservar, em uma transação, seu total, suas linhas obrigatórias e seu pagamento único. Os itens podem ser removidos enquanto a venda está em montagem, antes do registro do pagamento. Uma nova tentativa após rollback deve construir uma nova instância da venda, pois a anterior pode conter identificadores gerados na transação revertida.
- O DDL usa FKs e PKs para garantir, no lado dependente, a integridade dos itens e do pagamento. Escritas SQL diretas precisam garantir por conta própria a existência de pelo menos um item e um pagamento para cada venda.

## Unidade de persistência e DAOs

`vendasPU` é `RESOURCE_LOCAL` e usa o provider Hibernate, o dialeto PostgreSQL 9.5 e `hbm2ddl.auto=validate`. Aplique o DDL uma vez para preparar um esquema vazio. Sobre um esquema já provisionado, a inicialização apenas valida as tabelas e tipos mapeados. [DDL OLTP](OLTP/diagrama_oltp.sql)

| Variável | Uso |
| --- | --- |
| `VENDAS_JDBC_URL` | URL JDBC. Valor padrão `jdbc:postgresql://localhost:5432/vendas` |
| `VENDAS_JDBC_USER` | Usuário do OLTP |
| `VENDAS_JDBC_PASSWORD` | Senha fornecida no ambiente da execução |
| `VENDAS_SHOW_SQL` | `true` para ver SQL formatado no desenvolvimento |

`Jpa.abrirEntityManager()` reutiliza uma fábrica ao longo da execução. Feche os contextos abertos pela aplicação e chame `Jpa.fechar()` no encerramento. O construtor sem argumentos dos DAOs abre e fecha um contexto por operação. A escrita abre transação, executa `persist`, `merge` ou exclusão e faz `commit` após sucesso. Em caso de falha, faz rollback quando a transação ainda está ativa. A exceção JPA identifica a operação e a entidade e mantém o erro original como causa.

Para compor operações, passe o mesmo `EntityManager` aos DAOs e inicie uma transação antes da primeira escrita. O chamador comita ou faz rollback e fecha o contexto. Em caso de falha durante a escrita, o DAO marca a transação compartilhada para rollback. Navegue por associações `LAZY` dentro do contexto correspondente ou carregue o grafo necessário em uma consulta, como em JPQL-02.

O DAO de vendas usa bloqueio pessimista na linha da venda durante atualização e cancelamento. Antes de mesclar uma venda com status `CONCLUIDA`, consulta também o status atual no banco sob o bloqueio. Isso impede a reabertura por um contexto que carregou a venda antes de outro contexto cancelá-la.

| DAO | Métodos específicos |
| --- | --- |
| `ClienteDAO` | `buscarPorCpf`, `buscarPorCartaoFidelidade`, `listarComHistorico` |
| `FuncionarioDAO` | `buscarPorMatricula`, `listarPorUnidade`, `listarPorCargo` |
| `UnidadeFarmaciaDAO`, `DepartamentoDAO` | CRUD de `GenericDAO` |
| `CategoriaDAO` | `listarPorDepartamento` |
| `MedicamentoDAO` | `buscarPorCodigoBarras`, `listarMaisVendidos`, `listarNuncaVendidos`, `buscarComFiltros` |
| `VendaDAO` | `cancelar`, `listarPorCliente`, `listarComItensPorCliente`, `listarPorStatusEPeriodo`, `totalPorCliente`, `faturamentoPorUnidade`, `listarPaginadas`, `buscarComFiltros`, `contarConcluidasPorUnidade` |
| `ItemVendaDAO` | `listarPorVenda`, `valorPorCategoria` |
| `PagamentoDAO` | `buscarPorNumeroTransacao`, `listarPorFormaPagamento` |

## Catálogo JPQL e Criteria

O período usa início inclusivo e fim exclusivo. Os relatórios de faturamento, volume e valor usam vendas concluídas. O histórico preserva cancelamentos. As listas agregadas retornam uma linha por grupo com ID (`Long`), nome (`String`) e medida (`BigDecimal` para valores ou `Long` para contagens e quantidades).

| ID | Pergunta operacional e método | Parâmetros | Retorno e recursos |
| --- | --- | --- | --- |
| JPQL-01 | Quais vendas pertencem ao cliente? `VendaDAO.listarPorCliente` | `Long clienteId` | `List<Venda>`. `JOIN`, parâmetro nomeado e `@NamedQuery("Venda.porCliente")` |
| JPQL-02 | Quais são as linhas das compras do cliente? `VendaDAO.listarComItensPorCliente` | `Long clienteId` | `List<Venda>` com itens e pagamento carregados. `JOIN FETCH` e `DISTINCT` |
| JPQL-03 | Quais clientes atingiram certo total comprado? `VendaDAO.totalPorCliente` | `BigDecimal minimo` inclusivo | `List<Object[]>`. `SUM`, `GROUP BY` e `HAVING` |
| JPQL-04 | Quanto faturou cada unidade no período? `VendaDAO.faturamentoPorUnidade` | `Date inicio, fimExclusivo` | `List<Object[]>`. `JOIN`, `SUM` e `GROUP BY` |
| JPQL-05 | Quais medicamentos tiveram o maior volume vendido? `MedicamentoDAO.listarMaisVendidos` | Sem parâmetros públicos | `List<Object[]>`. `SUM`, `GROUP BY` e ordenação por volume |
| JPQL-06 | Quais vendas têm o status no período? `VendaDAO.listarPorStatusEPeriodo` | `StatusVenda status, Date inicio, fimExclusivo` | `List<Venda>`. Filtros nomeados e ordenação temporal |
| JPQL-07 | Quais medicamentos nunca apareceram em uma venda concluída? `MedicamentoDAO.listarNuncaVendidos` | Sem parâmetros públicos | `List<Medicamento>`. Subconsulta correlacionada com `NOT EXISTS` |
| JPQL-08 | Quais são as próximas vendas da listagem? `VendaDAO.listarPaginadas` | `int deslocamento, tamanho` | `List<Venda>`. `setFirstResult` e `setMaxResults`, ordem decrescente por data e ID |
| JPQL-09 | Quais clientes têm histórico registrado? `ClienteDAO.listarComHistorico` | Sem parâmetros | `List<Cliente>`. `JOIN`, `DISTINCT` e `@NamedQuery("Cliente.comHistorico")` |
| JPQL-10 | Qual é o total líquido vendido por categoria? `ItemVendaDAO.valorPorCategoria` | Sem parâmetros públicos | `List<Object[]>`. Duas junções, `SUM` e `GROUP BY` |
| Criteria-01 | Quais vendas combinam com os filtros informados? `VendaDAO.buscarComFiltros` | `Long clienteId, unidadeId, funcionarioId`, `StatusVenda status`, `Date inicio, fimExclusivo`, todos opcionais | `List<Venda>`. `CriteriaBuilder`, `Root`, lista dinâmica de `Predicate` |
| Criteria-02 | Quais medicamentos atendem à classificação informada? `MedicamentoDAO.buscarComFiltros` | `Long categoriaId, departamentoId`, `TipoControleMedicamento tipoControle`, todos opcionais | `List<Medicamento>`. Caminhos em associações e predicados opcionais |
| Criteria-03 | Quantas vendas concluídas ocorreram por unidade no período? `VendaDAO.contarConcluidasPorUnidade` | `Date inicio, fimExclusivo` | `List<Object[]>`. `Join`, `count`, `groupBy` |

O teste `PostgresqlTest.executaAsDezConsultasJpqlInclusiveNamedQueries` verifica JPQL-01 a JPQL-10. `PostgresqlTest.executaAsTresConsultasCriteriaComFiltrosCombinadosEOpcionais` verifica Criteria-01 a Criteria-03. O cenário inclui vendas concluídas e canceladas, empate de horários, um limite de período e um medicamento presente apenas em venda cancelada.

## Uso e testes

Com cliente, unidade, funcionário e medicamento já cadastrados, registre a venda com objetos recuperados em um DAO ou no contexto passado pelo chamador.

```java
Date agora = new Date();
Venda venda = new Venda(cliente, unidade, funcionario, agora);
venda.adicionarItem(1, medicamento, 2, medicamento.getPrecoBase(), new BigDecimal("1.50"));
venda.registrarPagamento(FormaPagamento.PIX, agora, numeroTransacao).confirmar();

VendaDAO vendas = new VendaDAO();
vendas.salvar(venda);
List<Venda> historicoComItens = vendas.listarComItensPorCliente(cliente.getId());
vendas.cancelar(venda.getId());
```

Um `EntityManager` compartilhado permite agrupar duas gravações numa transação.

```java
EntityManager em = Jpa.abrirEntityManager();
EntityTransaction tx = em.getTransaction();
try {
    tx.begin();
    new ClienteDAO(em).salvar(clienteNovo);
    new VendaDAO(em).salvar(vendaDoClienteNovo);
    tx.commit();
} catch (RuntimeException e) {
    if (tx.isActive()) {
        tx.rollback();
    }
    throw e;
} finally {
    em.close();
}
```

Execute estes comandos na pasta `backend` com Maven e JDK 8.

```powershell
java -version
mvn compile
mvn test
```

`mvn test` executa os testes de domínio. Para executar também as integrações, use um banco local dedicado a testes e uma conta com permissão para criar schemas. Informe uma URL JDBC base sem `currentSchema`. Cada teste cria um schema com nome aleatório, aplica o DDL existente, popula o cenário e remove o schema que criou no encerramento.

```powershell
$env:ORM_TEST_JDBC_URL = 'jdbc:postgresql://localhost:5432/vendas_teste'
$env:ORM_TEST_JDBC_USER = 'usuario_de_teste'
# Forneça ORM_TEST_JDBC_PASSWORD pela sessão quando exigida.
mvn -Ppostgresql test
```

O perfil de integração exige as variáveis de acesso. Asserções cobrem recuperação dos mapeamentos, CRUD, rollback após violação de unicidade, imutabilidade do preço histórico, cancelamento, resultados das consultas, datas e paginação. Os relatórios Surefire ficam em `backend/target/surefire-reports/`.

A validação local usou Amazon Corretto 8u512, Maven 3.9.16 e PostgreSQL 18.6. `mvn compile` compilou para Java 8. A execução final de `mvn -Ppostgresql package` aprovou 12 testes, sendo 3 unitários e 9 de integração, e gerou `backend/target/farmacia-orm-1.0-SNAPSHOT.jar`. O Maven usou um cache temporário local e executou esses comandos em modo offline depois da resolução das dependências.

## Demonstração na arguição

1. Abra `Pessoa` e `Cliente`. Mostre a PK em `pessoa` e a PK/FK em `cliente`. Consulte `em.find(Pessoa.class, clienteId)` e mostre o subtipo retornado.
2. Registre dois itens de uma venda, pague em PIX e mostre os registros em `venda`, `item_venda` e `pagamento`. Explique como o item guarda o preço praticado e como a chave compartilhada garante um pagamento por venda.
3. Execute `listarComItensPorCliente` com `VENDAS_SHOW_SQL=true` e mostre o carregamento de itens com a venda. Compare o SQL de Criteria-01 com filtros diferentes.
4. Cancele a venda. Confira sua presença no histórico e seu efeito nos resultados de faturamento. Use o teste de duplicidade de transação para mostrar o rollback do registro completo.
