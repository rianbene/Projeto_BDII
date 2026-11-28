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