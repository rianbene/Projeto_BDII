# Mineração de Dados: Regras de Associação (Algoritmo Apriori)

## 1. Preparação dos Dados

Para identificar padrões de compra conjunta (*market basket analysis*) na rede de farmácias, os dados foram extraídos do Data Warehouse (`dw_estrela`), cruzando a tabela fato `fato_venda` com a dimensão `dim_produto`.

* **Agrupamento por cesta de compras:** Como a granularidade da `fato_venda` representa um item por linha, os registros foram agrupados pela chave composta de data, cliente e unidade (`sk_tempo`, `sk_cliente`, `sk_loja`), totalizando **1.919 transações (cestas)**.
* **Matriz binária (`base_mineracao.csv`):** Por meio da visão `dw_estrela.vw_cesta_weka`, os **100 medicamentos** distintos foram pivotados em colunas, recebendo `1` quando presentes na compra e `0` quando ausentes.
* **Pré-processamento no Weka:** O arquivo `base_mineracao.csv` foi carregado no **Weka Explorer**. Na aba *Preprocess*, desmarcou-se o atributo alvo (*No class*) e aplicou-se o filtro `NumericToBinary` (`-R first-last`) para converter os valores numéricos $\{0, 1\}$ em atributos nominais binários.

---

## 2. Parâmetros Utilizados e Resultados

Na aba *Associate* do Weka, executou-se o algoritmo **Apriori** com a seguinte configuração:

* **`treatZeroAsMissing` (`-Z = True`):** Ignora os valores `0` (produtos não comprados) para gerar regras apenas sobre coocorrências positivas (`=1`).
* **`lowerBoundMinSupport` (`-M = 0.002`):** Suporte mínimo de $0,2\%$, exigindo ao menos **4 compras simultâneas** nas 1.919 cestas analisadas.
* **`minMetric` (`-C = 0.1`):** Confiança mínima de $10\%$.
* **`numRules` (`-N = 20`):** Retorno das 20 regras mais fortes ordenadas por confiança.

Ao final de 20 ciclos, o algoritmo encontrou **87 pares frequentes ($L_2$)** e gerou 20 regras com **Lift entre $3,00$ e $5,00$** (indicando forte associação positiva, acima do acaso). Principais regras obtidas:

| # | Antecedente (Compra inicial) | Consequente (Item associado) | Ocorrências | Confiança | Lift |
| :- | :--- | :--- | :---: | :---: | :---: |
| **1** | `vitamina_c_200mg_96_96` (47) | `simeticona_500mg_69_69` | 6 | $13\%$ | $3,60$ |
| **2** | `amoxicilina_400mg_83_83` (48) | `simeticona_500mg_19_19` | 6 | $13\%$ | $4,80$ |
| **3** | `amoxicilina_400mg_83_83` (48) | `ibuprofeno_300mg_22_22` | 6 | $13\%$ | $4,44$ |
| **4** | `simeticona_500mg_9_9` (40) | `amoxicilina_400mg_73_73` | 5 | $13\%$ | $5,00$ |
| **5** | `omeprazol_100mg_75_75` (41) | `ibuprofeno_300mg_12_12` | 5 | $12\%$ | $4,11$ |
| **10** | `paracetamol_200mg_31_31` (45) | `simeticona_500mg_39_39` | 5 | $11\%$ | $3,61$ |
| **12** | `paracetamol_200mg_1_1` (47) | `vitamina_c_200mg_6_6` | 5 | $11\%$ | $3,93$ |
| **15** | `azitromicina_400mg_88_88` (47) | `loratadina_300mg_47_47` | 5 | $11\%$ | $3,78$ |

---

## 3. Conclusões de Negócio

1. **Venda cruzada entre antibióticos e medicamentos gastrointestinais (Regras 2, 4, 6 e 19):**
   A associação entre `amoxicilina_400mg` e `simeticona_500mg` apresentou confiança de até $13\%$ e *lift* de até $5,00$ (chance cinco vezes maior de compra conjunta em relação à média geral). Como antibióticos frequentemente causam desconforto abdominal, os clientes tendem a levar antifiséticos na mesma compra. **Ação:** orientar os balconistas a ofertar antifiséticos e protetores gástricos na dispensação de antibióticos.

2. **Complementaridade entre anti-inflamatórios e protetores gástricos (Regra 5):**
   Observou-se que $12\%$ das cestas com `omeprazol_100mg_75` também incluíram `ibuprofeno_300mg_12` ($\text{Lift} = 4,11$), refletindo o uso combinado para proteger a mucosa gástrica durante tratamentos com anti-inflamatórios (AINEs). **Ação:** configurar alertas de venda sugestiva no sistema de caixa (PDV) sempre que um anti-inflamatório for registrado isoladamente.

3. **Kits sazonais para sintomas gripais e alérgicos (Regras 3, 11, 12, 14, 15 e 18):**
   Há forte coocorrência entre analgésicos (`paracetamol_200mg`, `dipirona_100mg`), antialérgicos (`loratadina_300mg`), antibióticos (`azitromicina_400mg`) e suplementos (`vitamina_c_200mg`), com *lift* entre $3,65$ e $4,59$. **Ação:** montar ilhas promocionais sazonais próximas aos caixas agrupando analgésicos, vitamina C e antialérgicos isentos de prescrição para aumentar o ticket médio.



# Consultas OLAP e Comparativo de Desempenho (OLTP vs. Estrela vs. Floco de Neve)

## 1. Operações OLAP Implementadas

Foram elaboradas e executadas **10 consultas analíticas** sobre o esquema dimensional `dw_estrela` no PostgreSQL (duas para cada operação OLAP exigida), explorando as dimensões `dim_tempo`, `dim_produto`, `dim_cliente` e `dim_loja` sobre a tabela fato `fato_venda`:

* **Roll-up (2 consultas):** Agregação hierárquica com `GROUP BY ROLLUP` subindo de *Mês → Trimestre → Ano → Total Geral* e de *Loja → Bairro → Cidade → Estado → Total da Rede*.
* **Drill-down (2 consultas):** Detalhamento progressivo descendo da visão anual para *Mês e Categoria de Medicamento*, e de *Estado/Cidade* para *Faixa Etária e Princípio Ativo*.
* **Slice (2 consultas):** Fatiamento do cubo fixando um único valor dimensional por vez (`semestre = 1` e `faixa_etaria = '30 A 44 ANOS'`).
* **Dice (2 consultas):** Recorte de subcubos combinando múltiplos filtros simultâneos (princípios ativos analgésicos + 1º e 2º trimestres + público de 30 a 59 anos; e vendas com desconto em dias úteis nos anos de 2025 e 2026).
* **Pivot (2 consultas):** Tabulação cruzada utilizando agregação condicional (`FILTER (WHERE ...)`), transformando *Trimestres* e *Faixas Etárias* em colunas comparativas por categoria e por unidade da farmácia.

---

## 2. Comparativo de Desempenho: OLTP vs. Estrela vs. Floco de Neve

Para avaliar o impacto da modelagem dimensional no tempo de resposta das consultas analíticas, a mesma consulta de negócio — **total de itens vendidos e faturamento líquido agrupados por Estado da loja, Categoria do medicamento e Ano** — foi executada nos três ambientes do banco de dados (`public`, `dw_estrela` e `dw_floco_neve`).

### Metodologia do Benchmark
Utilizou-se um script procedural (`PL/pgSQL`) com `EXPLAIN (ANALYZE, FORMAT JSON)` no PostgreSQL. Após uma execução inicial de aquecimento de cache (*warm-up*) descartada da medição, cada consulta foi executada **2.000 vezes** para extrair o tempo médio de planejamento do otimizador (*Planning Time*), o tempo médio de execução (*Execution Time*) e os valores mínimo e máximo registrados.

### Resultados Obtidos (2.000 execuções por modelo)

| Modelo Avaliado | Qtd. de JOINs | Execuções | Planejamento Médio (ms) | Execução Média (ms) | Total Médio (ms) | Execução Mín. (ms) | Execução Máx. (ms) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **1. OLTP (Normalizado)** | 4 | 2.000 | 0,539 | 10,735 | 11,274 | 8,085 | 59,388 |
| **2. DW Estrela** | **3** | **2.000** | **0,506** | **7,371** | **7,877** | **5,709** | **24,012** |
| **3. DW Floco de Neve** | 6 | 2.000 | 1,131 | 11,853 | 12,985 | 9,272 | 48,127 |

### Análise Técnica dos Resultados

1. **Superioridade do Modelo Estrela (`7,371 ms`):** O modelo estrela apresentou o menor tempo médio em todas as métricas, sendo **31,3% mais rápido na execução** do que o OLTP (`10,735 ms`) e **37,8% mais rápido** do que o Floco de Neve (`11,853 ms`), além de apresentar a menor latência de pico (`24,012 ms`). Esse ganho decorre da desnormalização das dimensões, que exige apenas **3 junções** diretas com a `fato_venda`, elimina a extração dinâmica de data (`EXTRACT(YEAR FROM data_hora)`) feita no OLTP e já armazena apenas vendas concluídas.
2. **Custo de Planejamento e Junções no Floco de Neve (`12,985 ms`):** A normalização das hierarquias de localização (`dim_loja → dim_cidade → dim_estado`) e produto (`dim_produto → dim_categoria`) dobrou o número de junções necessárias em relação ao modelo estrela (**6 JOINs contra 3 JOINs**). Como consequência, o tempo médio de planejamento do otimizador do PostgreSQL mais que dobrou (`1,131 ms` contra `0,506 ms`), tornando a consulta mais lenta até mesmo que no esquema OLTP para o volume atual de registros.
3. **Sobrecarga Transacional no OLTP (`11,274 ms`):** Embora possua 4 junções, o ambiente transacional exige leitura de tabelas altamente normalizadas (`item_venda`, `venda`, `unidade_farmacia`, `medicamento`, `categoria`), filtragem de status em tempo de execução e cálculo de atributos temporais linha a linha, penalizando o desempenho analítico.