-- Roll-up 1: Faturamento e volume subindo de Mês -> Trimestre -> Ano -> Total Geral
SELECT
    t.ano,
    t.trimestre,
    t.mes,
    SUM(f.quantidade) AS qtd_itens,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
GROUP BY ROLLUP (t.ano, t.trimestre, t.mes)
ORDER BY t.ano NULLS LAST, t.trimestre NULLS LAST, t.mes NULLS LAST;

-- Roll-up 2: Faturamento subindo de Loja -> Bairro -> Cidade -> Estado -> Total Rede
SELECT
    l.estado,
    l.cidade,
    l.bairro,
    l.nome_fantasia AS loja,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
GROUP BY ROLLUP (l.estado, l.cidade, l.bairro, l.nome_fantasia)
ORDER BY l.estado NULLS LAST, l.cidade NULLS LAST, l.bairro NULLS LAST, l.nome_fantasia NULLS LAST;


-- Drill-down 1: Abrindo o faturamento de Ano -> Trimestre -> Mês -> Categoria do Produto
SELECT
    t.ano,
    t.trimestre,
    t.mes,
    t.nome_mes,
    p.categoria,
    SUM(f.quantidade) AS qtd_vendida,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
GROUP BY t.ano, t.trimestre, t.mes, t.nome_mes, p.categoria
ORDER BY t.ano, t.mes, faturamento_liquido DESC;

-- Drill-down 2: Detalhando vendas por Estado -> Cidade -> Faixa Etária -> Princípio Ativo
SELECT
    l.estado,
    l.cidade,
    c.faixa_etaria,
    p.principio_ativo,
    COUNT(f.sk_fato) AS total_registros,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
JOIN dw_estrela.dim_cliente c ON c.sk_cliente = f.sk_cliente
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
GROUP BY l.estado, l.cidade, c.faixa_etaria, p.principio_ativo
ORDER BY l.estado, l.cidade, c.faixa_etaria, faturamento_liquido DESC;


-- Slice 1: Fixando apenas o 1º Semestre (semestre = 1)
SELECT
    p.categoria,
    l.nome_fantasia AS loja,
    SUM(f.quantidade) AS qtd_vendida,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
WHERE t.semestre = 1
GROUP BY p.categoria, l.nome_fantasia
ORDER BY faturamento_liquido DESC;

-- Slice 2: Fixando apenas clientes da faixa etária '30 A 44 ANOS'
SELECT
    p.principio_ativo,
    p.categoria,
    SUM(f.quantidade) AS qtd_comprada,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_cliente c ON c.sk_cliente = f.sk_cliente
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
WHERE c.faixa_etaria = '30 A 44 ANOS'
GROUP BY p.principio_ativo, p.categoria
ORDER BY qtd_comprada DESC;


-- Dice 1: Analgésicos/Antitérmicos + 1º e 2º Trimestres + Adultos (30 a 59 anos)
SELECT
    l.nome_fantasia AS loja,
    t.trimestre,
    p.principio_ativo,
    SUM(f.quantidade) AS qtd_vendida,
    SUM(f.valor_liquido) AS faturamento_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
JOIN dw_estrela.dim_cliente c ON c.sk_cliente = f.sk_cliente
JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
WHERE (p.principio_ativo ILIKE '%Paracetamol%' OR p.principio_ativo ILIKE '%Dipirona%')
  AND t.trimestre IN (1, 2)
  AND c.faixa_etaria IN ('30 A 44 ANOS', '45 A 59 ANOS')
GROUP BY l.nome_fantasia, t.trimestre, p.principio_ativo
ORDER BY l.nome_fantasia, t.trimestre, faturamento_liquido DESC;

-- Dice 2: Vendas com desconto aplicado + Dias úteis + Anos 2025 e 2026
SELECT
    p.categoria,
    l.cidade,
    SUM(f.valor_bruto) AS valor_bruto,
    SUM(f.desconto) AS total_descontos,
    SUM(f.valor_liquido) AS valor_liquido
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
WHERE t.ano IN (2025, 2026)
  AND TRIM(t.dia_semana) NOT IN ('Saturday', 'Sunday', 'Sábado', 'Domingo')
  AND f.desconto > 0
GROUP BY p.categoria, l.cidade
ORDER BY total_descontos DESC;


-- Pivot 1: Categorias nas linhas x Trimestres nas colunas
SELECT
    p.categoria,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE t.trimestre = 1), 0) AS tri_1,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE t.trimestre = 2), 0) AS tri_2,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE t.trimestre = 3), 0) AS tri_3,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE t.trimestre = 4), 0) AS tri_4,
    SUM(f.valor_liquido) AS total_geral
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_produto p ON p.sk_produto = f.sk_produto
JOIN dw_estrela.dim_tempo t ON t.sk_tempo = f.sk_tempo
GROUP BY p.categoria
ORDER BY total_geral DESC;

-- Pivot 2: Unidades da Farmácia nas linhas x Faixa Etária dos Clientes nas colunas
SELECT
    l.nome_fantasia AS loja,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE c.faixa_etaria = 'MENOR DE 18'), 0) AS menor_18,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE c.faixa_etaria = '18 A 29 ANOS'), 0) AS anos_18_29,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE c.faixa_etaria = '30 A 44 ANOS'), 0) AS anos_30_44,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE c.faixa_etaria = '45 A 59 ANOS'), 0) AS anos_45_59,
    COALESCE(SUM(f.valor_liquido) FILTER (WHERE c.faixa_etaria = '60 ANOS OU MAIS'), 0) AS anos_60_mais,
    SUM(f.valor_liquido) AS total_loja
FROM dw_estrela.fato_venda f
JOIN dw_estrela.dim_loja l ON l.sk_loja = f.sk_loja
JOIN dw_estrela.dim_cliente c ON c.sk_cliente = f.sk_cliente
GROUP BY l.nome_fantasia
ORDER BY total_loja DESC;