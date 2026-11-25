# Modelo Estrela (Star Schema) — Rede de Farmácias

## O que é
Modelo dimensional em que **uma tabela fato central** se liga diretamente a **dimensões desnormalizadas**.
Prioriza performance de leitura e simplicidade: poucas junções (`fato → dimensão`), ideal para consultas OLAP
(`SUM`, `GROUP BY`, *roll-up* e *drill-down*).

## Como foi implementado
- **Esquema:** `dw` (separado do esquema OLTP `public`, que é gerado pelo JPA/Hibernate do Spring Boot).
- **Granularidade:** 1 linha = **um item (medicamento) vendido em uma venda**.
- **Fato:** `dw.fato_venda`
  - Medidas: `quantidade`, `valor_bruto`, `desconto`, `valor_liquido`.
  - Chaves estrangeiras: `sk_tempo`, `sk_produto`, `sk_cliente`, `sk_loja`.
- **Dimensões (4, todas planas):**
  | Dimensão | Origem no OLTP | Atributos de análise |
  |---|---|---|
  | `dim_tempo` | `venda.data_venda` | dia, mês, nome_mes, trimestre, semestre, ano, dia_semana |
  | `dim_produto` | `produto` | nome, principio_ativo, categoria, fabricante |
  | `dim_cliente` | `cliente` | nome, cidade, estado, data_nascimento, faixa_etaria |
  | `dim_loja` | `loja` | nome_fantasia, bairro, cidade, estado |
- **Chaves:** `sk_*` substitutas (surrogate) no DW; `id_origem` guarda a chave natural do OLTP para rastreabilidade do ETL.
- **Carga:** ETL lê `venda` ⋈ `item_venda` ⋈ `produto`/`cliente`/`loja` no OLTP e grava no `dw`.

## Arquivos
- `ddl_estrela.sql` — criação do esquema, tabelas e índices.
- `diagrama_estrela.md` — diagrama em Mermaid.

## Como executar
```bash
psql -U <usuario> -d <banco> -f ddl_estrela.sql
```

## Consulta de validação
```sql
SELECT t.ano, t.mes, p.categoria, SUM(f.valor_liquido) AS faturamento
FROM dw.fato_venda f
JOIN dw.dim_tempo   t ON t.sk_tempo   = f.sk_tempo
JOIN dw.dim_produto p ON p.sk_produto = f.sk_produto
GROUP BY t.ano, t.mes, p.categoria
ORDER BY t.ano, t.mes;
```

## Critérios de aceite
- [ ] Esquema `dw` criado, sem tabelas no esquema OLTP.
- [ ] 1 fato com ≥ 3 medidas e ≥ 4 dimensões, incluindo Tempo com todos os atributos exigidos.
- [ ] Toda FK da fato resolve para uma dimensão (zero linhas órfãs).
- [ ] `SUM(valor_liquido)` no DW = `SUM` equivalente no OLTP para o mesmo período.
- [ ] `valor_liquido = valor_bruto - desconto` em 100% das linhas.