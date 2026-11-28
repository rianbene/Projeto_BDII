DROP TABLE IF EXISTS temp_benchmark;
CREATE TEMP TABLE temp_benchmark (
    modelo VARCHAR(40),
    juncoes INT,
    planning_ms NUMERIC(10,4),
    execution_ms NUMERIC(10,4)
);

DO $$
DECLARE
    i INT;
    n_execucoes INT := 2000; 
    plano JSON;
    q_oltp TEXT := '
        SELECT u.estado, cat.nome, EXTRACT(YEAR FROM v.data_hora)::SMALLINT,
               SUM(iv.quantidade), SUM(iv.valor_liquido)
        FROM public.item_venda iv
        JOIN public.venda v ON v.id = iv.venda_id
        JOIN public.unidade_farmacia u ON u.id = v.unidade_id
        JOIN public.medicamento m ON m.id = iv.medicamento_id
        JOIN public.categoria cat ON cat.id = m.categoria_id
        WHERE v.status = ''CONCLUIDA''
        GROUP BY u.estado, cat.nome, EXTRACT(YEAR FROM v.data_hora)';

    q_estrela TEXT := '
        SELECT l.estado, p.categoria, t.ano,
               SUM(f.quantidade), SUM(f.valor_liquido)
        FROM dw_estrela.fato_venda f
        JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
        JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
        JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
        GROUP BY l.estado, p.categoria, t.ano';

    q_floco TEXT := '
        SELECT e.sigla, cat.nome, t.ano,
               SUM(f.quantidade), SUM(f.valor_liquido)
        FROM dw_floco_neve.fato_venda f
        JOIN dw_floco_neve.dim_tempo t ON t.sk_tempo = f.sk_tempo
        JOIN dw_floco_neve.dim_loja l ON l.sk_loja = f.sk_loja
        JOIN dw_floco_neve.dim_cidade cid ON cid.sk_cidade = l.sk_cidade
        JOIN dw_floco_neve.dim_estado e ON e.sk_estado = cid.sk_estado
        JOIN dw_floco_neve.dim_produto p ON p.sk_produto = f.sk_produto
        JOIN dw_floco_neve.dim_categoria cat ON cat.sk_categoria = p.sk_categoria
        GROUP BY e.sigla, cat.nome, t.ano';
BEGIN
    EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON) ' || q_oltp INTO plano;
    EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON) ' || q_estrela INTO plano;
    EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON) ' || q_floco INTO plano;

    FOR i IN 1..n_execucoes LOOP
        EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON) ' || q_oltp INTO plano;
        INSERT INTO temp_benchmark VALUES (
            '1. OLTP (Normalizado)', 4,
            (plano->0->>'Planning Time')::NUMERIC,
            (plano->0->>'Execution Time')::NUMERIC
        );

        EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON) ' || q_estrela INTO plano;
        INSERT INTO temp_benchmark VALUES (
            '2. DW Estrela', 3,
            (plano->0->>'Planning Time')::NUMERIC,
            (plano->0->>'Execution Time')::NUMERIC
        );

        EXECUTE 'EXPLAIN (ANALYZE, FORMAT JSON) ' || q_floco INTO plano;
        INSERT INTO temp_benchmark VALUES (
            '3. DW Floco de Neve', 6,
            (plano->0->>'Planning Time')::NUMERIC,
            (plano->0->>'Execution Time')::NUMERIC
        );
    END LOOP;
END $$;

SELECT
    modelo,
    juncoes AS qtd_joins,
    COUNT(*) AS execucoes,
    ROUND(AVG(planning_ms), 3) AS planejamento_medio_ms,
    ROUND(AVG(execution_ms), 3) AS execucao_media_ms,
    ROUND(AVG(planning_ms + execution_ms), 3) AS total_medio_ms,
    ROUND(MIN(execution_ms), 3) AS min_execucao_ms,
    ROUND(MAX(execution_ms), 3) AS max_execucao_ms
FROM temp_benchmark
GROUP BY modelo, juncoes
ORDER BY modelo;