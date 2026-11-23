# Pipeline ETL da Rede de Farmácias

O ETL extrai o OLTP do esquema `public`, transforma uma fotografia em `staging` e carrega os modelos `dw_estrela` e `dw_floco_neve` na mesma transação PostgreSQL. A granularidade é um item por venda. A execução usa SQL e PL/pgSQL, com scripts de entrada para `psql`.

## Execução

O banco deve conter as tabelas operacionais da aplicação, conforme `docs/OLTP/diagrama_oltp.sql`. Use a mesma base PostgreSQL para os três ambientes lógicos. A conta precisa ler o OLTP e criar ou atualizar os objetos de staging, controle e DW.

Na raiz do projeto, execute a instalação uma vez e a carga sempre que houver novos dados operacionais.

```powershell
$psqlEtl = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psqlEtl -X -h localhost -p 5432 -U seu_usuario -d seu_banco -W -f data_warehouse/etl/instalar.sql
& $psqlEtl -X -h localhost -p 5432 -U seu_usuario -d seu_banco -W -f data_warehouse/etl/executar.sql
```

`-W` solicita a senha na execução. Os scripts interrompem o processamento no primeiro erro. `instalar.sql` pode ser repetido e aplica migrações aditivas sobre as tabelas existentes. Os DDLs da equipe passaram a usar `IF NOT EXISTS`, preservando os dados em uma reinstalação.

| Arquivo | Responsabilidade |
| --- | --- |
| `etl/instalar.sql` | Instala os dois modelos, a estrutura do ETL e suas funções em uma transação |
| `etl/01_estrutura.sql` | Cria staging, auditoria, identificação da origem, índices e colunas SCD2 |
| `etl/02_pipeline.sql` | Extrai, transforma, carrega e reconcilia os dois modelos |
| `etl/executar.sql` | Abre transação REPEATABLE READ, executa a carga e apresenta as contagens |
| `tests/etl_integracao.sql` | Valida os cenários com um banco vazio dedicado |

Para usar a função diretamente em uma ferramenta SQL, execute o bloco completo.

```sql
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga();
COMMIT;
```

Use o mesmo fuso de sessão adotado pela aplicação para interpretar `venda.data_hora`, que é `TIMESTAMP` sem fuso. A função aceita uma referência explícita para demonstrações reproduzíveis. Ela deve ser posterior à referência da última carga concluída.

## Staging e transformação

- `staging.cliente` combina `cliente` e `pessoa`.
- `staging.produto` combina `medicamento`, `categoria` e `departamento`. O fabricante vem de `medicamento.laboratorio`.
- `staging.loja` vem de `unidade_farmacia`.
- `staging.item_venda` combina `venda` e `item_venda`, incluindo cancelamentos.
- `staging.tempo` gera todos os dias dos anos cobertos pelas vendas. Com origem vazia, gera o ano da referência da carga.

O calendário contém dia, mês, nome do mês, trimestre, semestre, ano e dia da semana em português. A chave de tempo usa `AAAAMMDD`. Anos carregados anteriormente permanecem disponíveis.

Os textos passam por remoção de espaços nas bordas, compactação de espaços internos, tabulações e quebras de linha e conversão por `UPPER`. A conversão de caixa segue as regras de collation do banco. Valores descritivos nulos ou vazios recebem `NAO INFORMADO`. UFs inválidas ou ausentes recebem `NI`. As faixas etárias são calculadas na referência da carga.

O desconto nulo é convertido em zero. As medidas são calculadas a partir dos valores praticados no item.

```text
valor_bruto = quantidade * preco_unitario
valor_liquido = valor_bruto - desconto
```

O desconto é absoluto por linha. O pipeline calcula o líquido independentemente do campo derivado armazenado no OLTP. Quantidade, data e preço ausentes, medidas negativas e desconto acima do bruto interrompem a carga. Valores financeiros continuam sujeitos à precisão `NUMERIC(12,2)` da fato.

Os membros desconhecidos usam SK zero e `id_origem = -1`. Os identificadores gerados pela aplicação são positivos. Uma referência ausente resolve para esse membro, mantendo a integridade das FKs. A versão do cliente desconhecido tem vigência inicial ilimitada.

## Carga incremental e SCD Tipo 2

As dimensões usam suas chaves `SERIAL`. `id_origem` identifica produtos, lojas e clientes do OLTP. As hierarquias do floco usam os nomes normalizados de departamento e categoria, e as combinações cidade/UF, de acordo com as unicidades do modelo da equipe.

Produto e loja usam atualização Tipo 1. A dimensão cliente usa SCD Tipo 2 para nome, cidade, estado, nascimento e faixa etária.

| Coluna | Uso |
| --- | --- |
| `data_inicio` | Início inclusivo da versão |
| `data_fim` | Fim exclusivo. NULL identifica a versão atual |
| `atual` | Indica a versão corrente, com índice que garante uma por cliente |

Ao detectar mudança, a carga encerra a versão atual na referência de extração e insere uma nova linha com outra SK. Os fatos selecionam a versão cuja vigência contém o horário da venda. Vendas antigas que chegam depois também usam o intervalo correspondente. Alterações apenas de caixa ou espaços são normalizadas antes da comparação.

O OLTP disponibiliza somente o cadastro atual. A primeira versão recebe `data_inicio = '-infinity'`, permitindo carregar as vendas anteriores à implantação do ETL. Isso representa a hipótese de que o primeiro cadastro observado atende a esse histórico. As versões posteriores começam quando a mudança é detectada. Mudanças ocorridas e revertidas entre duas execuções exigem histórico ou CDC na origem para serem reconstruídas.

A chave de negócio da fato é `(venda_id_origem, numero_item)`. O UPSERT insere novos itens e atualiza apenas linhas com diferenças. `execucao_id` registra a carga que inseriu ou alterou cada fato. Uma repetição com a mesma origem preserva as SKs, a quantidade de linhas e o identificador de carga dos fatos inalterados. As sequências podem avançar durante tentativas de inserção, portanto suas SKs podem conter intervalos.

Cancelamentos atualizam `status_venda`. Exclusões na origem marcam `excluido_origem`, preservando a linha histórica. Se o item reaparecer, a carga reativa a linha existente. As views `dw_estrela.vendas_concluidas` e `dw_floco_neve.vendas_concluidas` selecionam as linhas que participam do faturamento.

A extração lê toda a fotografia, pois o OLTP atual não possui um marcador confiável de alteração. A atualização dos destinos é incremental por comparação. Isso também identifica cancelamentos e correções de vendas antigas.

## Auditoria e consistência

`etl.execucao` registra a referência, o início e o fim da carga concluída. `etl.contagem` armazena `COUNT(*)` antes e depois para cada tabela operacional, de staging e dos dois modelos.

```sql
SELECT e.id, e.referencia, c.esquema, c.tabela,
       c.antes, c.depois, c.depois - c.antes AS diferenca
FROM etl.execucao e
JOIN etl.contagem c ON c.execucao_id = e.id
ORDER BY e.id, c.esquema, c.tabela;

SELECT id_origem, sk_cliente, cidade, estado, data_inicio, data_fim, atual
FROM dw_estrela.dim_cliente
ORDER BY id_origem, data_inicio;

SELECT SUM(valor_liquido) AS faturamento
FROM dw_estrela.vendas_concluidas;
```

A fotografia usa REPEATABLE READ e a execução obtém um advisory lock transacional. Outra carga concorrente recebe uma mensagem para tentar novamente ao término. A instalação usa o mesmo lock.

Antes de concluir, o pipeline compara o número de itens e o valor líquido das vendas concluídas em staging com cada destino. Uma divergência interrompe a transação. Se houver erro, PostgreSQL desfaz staging, dimensões, fatos e auditoria dessa tentativa. O cliente recebe o erro para diagnóstico e pode repetir a execução. O log de sucessos corresponde às cargas efetivamente confirmadas. [Isolamento transacional PostgreSQL](https://www.postgresql.org/docs/18/transaction-iso.html) · [Advisory locks](https://www.postgresql.org/docs/18/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS) · [UPSERT](https://www.postgresql.org/docs/18/sql-insert.html)

Uma fato criada antes do ETL pode estar sem `venda_id_origem` e `numero_item`. A carga detecta essa situação e exige a reconciliação desses campos. Cadastros antigos com duplicidade de `id_origem` também precisam ser reconciliados para criar os índices de unicidade.

## Validação

Crie um banco vazio dedicado. O teste recusa um banco que já contenha as tabelas operacionais ou os esquemas do ETL.

```powershell
& $psqlEtl -X -h localhost -p 5432 -U seu_usuario -d banco_vazio_de_testes -W -f data_warehouse/tests/etl_integracao.sql
```

O roteiro cobre carga vazia e inicial, calendário bissexto, nulos, normalização, limites dos textos, medidas, contagens, reinstalação, idempotência, SCD2, chegada atrasada, cancelamento, exclusão e reaparecimento, rollback nos dois modelos, rejeição de medidas inválidas, referências ausentes, hierarquias e reconciliação financeira. As verificações emitem `PASSOU` e interrompem o teste se uma condição falhar.

Na validação de 22/11/2026 com PostgreSQL 18.6, as 41 verificações SQL passaram. O teste em duas sessões confirmou a rejeição de uma carga concorrente. A entrada normal `executar.sql` concluiu a transação e apresentou as 29 contagens de tabelas. Os registros locais de execução ficam em `backend/target/etl-tests/`.

Estas contagens foram registradas nos dois modelos com os dados fictícios do teste. A dimensão cliente inclui o membro desconhecido.

| Cenário | Clientes antes/depois | Fatos antes/depois |
| --- | --- | --- |
| Primeira carga com dados | 1 / 3 | 0 / 3 |
| Repetição da mesma origem | 3 / 3 | 3 / 3 |
| Mudança de cidade de um cliente | 3 / 4 | 3 / 3 |
