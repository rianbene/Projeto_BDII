# Modelo Floco de Neve (Snowflake Schema) — Rede de Farmácias

## O que é
Variação do modelo estrela em que **as dimensões são normalizadas** em tabelas auxiliares, formando
hierarquias. Elimina redundância e facilita a manutenção dos atributos hierárquicos, ao custo de
mais junções nas consultas.

## Como foi implementado
Mesmo **fato, granularidade e medidas** do modelo estrela — apenas as dimensões mudam de forma.

- **Esquema:** `dw` | **Granularidade:** 1 item vendido por venda.
- **Fato:** `dw.fato_venda` (idêntica à do estrela: `quantidade`, `valor_bruto`, `desconto`, `valor_liquido`).
- **Dimensões normalizadas (2):**
  - **Produto → Categoria → Departamento**
    `dim_produto.sk_categoria → dim_categoria.sk_departamento → dim_departamento`
  - **Loja → Cidade → Estado**
    `dim_loja.sk_cidade → dim_cidade.sk_estado → dim_estado`
- **Dimensões mantidas planas:** `dim_tempo` e `dim_cliente` (atributos sem hierarquia redundante relevante).
- **Atributos migrados:** `categoria`/`fabricante` saem de `dim_produto`; `cidade`/`estado` saem de `dim_loja`.
- **Chaves:** `sk_*` substitutas em todos os níveis; `id_origem` preservado em `dim_produto` e `dim_loja`.

> Observação: os dois modelos são alternativas para o **mesmo assunto** (venda de medicamentos).
> Para execução simultânea, use esquemas distintos (`dw` e `dw_snowflake`) ou recrie o `dw` antes de cada carga.

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
FROM dw.fato_venda f
JOIN dw.dim_loja   l ON l.sk_loja   = f.sk_loja
JOIN dw.dim_cidade c ON c.sk_cidade = l.sk_cidade
JOIN dw.dim_estado e ON e.sk_estado = c.sk_estado
GROUP BY e.sigla, c.nome
ORDER BY e.sigla, faturamento DESC;
```

## Critérios de aceite
- [ ] Pelo menos 2 dimensões normalizadas em ≥ 2 níveis de hierarquia.
- [ ] Fato inalterada em relação ao estrela (mesmas medidas e granularidade).
- [ ] Nenhum atributo hierárquico duplicado nas dimensões folha.
- [ ] Faturamento total idêntico ao do modelo estrela para a mesma carga.
- [ ] Consulta de *drill-down* Estado → Cidade → Loja retorna os mesmos valores do OLTP.