DO $$
DECLARE
    colunas TEXT;
BEGIN
    SELECT STRING_AGG(
        FORMAT(
            'MAX(CASE WHEN f.sk_produto = %s THEN 1 ELSE 0 END) AS %I',
            p.sk_produto,
            LEFT(REGEXP_REPLACE(LOWER(p.nome), '[^a-z0-9]+', '_', 'g'), 55) || '_' || p.sk_produto
        ),
        ', '
    )
    INTO colunas
    FROM dw_estrela.dim_produto p
    WHERE EXISTS (SELECT 1 FROM dw_estrela.fato_venda f WHERE f.sk_produto = p.sk_produto);

    EXECUTE 'DROP VIEW IF EXISTS dw_estrela.vw_cesta_weka';
    EXECUTE 'CREATE VIEW dw_estrela.vw_cesta_weka AS SELECT ' || colunas ||
            ' FROM dw_estrela.fato_venda f GROUP BY f.sk_tempo, f.sk_cliente, f.sk_loja';
END $$;

SELECT * FROM dw_estrela.vw_cesta_weka;