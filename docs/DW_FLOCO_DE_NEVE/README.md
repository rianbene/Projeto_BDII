# Modelo Floco de Neve (Snowflake Schema) — Rede de Farmácias

## O que é
Variação do modelo estrela em que **as dimensões são normalizadas** em tabelas auxiliares, formando
hierarquias. Elimina redundância e facilita a manutenção dos atributos hierárquicos, ao custo de
mais junções nas consultas.

## Como foi implementado
Mesmo **fato, granularidade e medidas** do modelo estrela — apenas as dimensões mudam de forma.

- **Esquema:** `dw_floco_neve` | **Granularidade:** 1 item vendido por venda.
- **Fato:** `dw_floco_neve.fato_venda`, com as mesmas medidas do estrela.
- **Dimensões normalizadas (2):**
  - **Produto → Categoria → Departamento**
    `dim_produto.sk_categoria → dim_categoria.sk_departamento → dim_departamento`
  - **Loja → Cidade → Estado**
    `dim_loja.sk_cidade → dim_cidade.sk_estado → dim_estado`
- **Dimensões mantidas planas:** `dim_tempo` e `dim_cliente` (atributos sem hierarquia redundante relevante).
- **Atributos migrados:** `categoria`/`fabricante` saem de `dim_produto`; `cidade`/`estado` saem de `dim_loja`.
- **Chaves:** `sk_*` substitutas em todos os níveis; `id_origem` preservado em `dim_produto` e `dim_loja`.

Os dois modelos representam o mesmo assunto e são carregados simultaneamente em `dw_estrela` e `dw_floco_neve`. A instalação do ETL acrescenta rastreabilidade por item e SCD Tipo 2 no cliente. [Execução do ETL](../../data_warehouse/README.md)

## Arquivos
- `ddl_floco_de_neve.sql` — criação do esquema, tabelas hierárquicas e índices.
- `diagrama_floco_de_neve.md` — diagrama em Mermaid.

## Como executar
```bash
psql -U <usuario> -d <banco> -f ddl_floco_de_neve.sql
```

## Consulta de validação (drill-down geográfico)
```sql
SELECT e.sigla AS estado, c.nome AS cidade, SUM(f.valor_liquido) AS faturamento
FROM dw_floco_neve.vendas_concluidas f
JOIN dw_floco_neve.dim_loja   l ON l.sk_loja   = f.sk_loja
JOIN dw_floco_neve.dim_cidade c ON c.sk_cidade = l.sk_cidade
JOIN dw_floco_neve.dim_estado e ON e.sk_estado = c.sk_estado
GROUP BY e.sigla, c.nome
ORDER BY e.sigla, faturamento DESC;
```

## Critérios de aceite
- [ ] Pelo menos 2 dimensões normalizadas em ≥ 2 níveis de hierarquia.
- [ ] Fato inalterada em relação ao estrela (mesmas medidas e granularidade).
- [ ] Nenhum atributo hierárquico duplicado nas dimensões folha.
- [ ] Faturamento total idêntico ao do modelo estrela para a mesma carga.
- [ ] Consulta de *drill-down* Estado → Cidade → Loja retorna os mesmos valores do OLTP.
