\set ON_ERROR_STOP on
\encoding UTF8

-- Use um banco dedicado e vazio. A guarda impede o uso acidental sobre um OLTP existente.
DO $guarda$
BEGIN
    IF to_regclass('public.venda') IS NOT NULL
       OR to_regnamespace('dw_estrela') IS NOT NULL
       OR to_regnamespace('dw_floco_neve') IS NOT NULL
       OR to_regnamespace('etl') IS NOT NULL
       OR to_regnamespace('staging') IS NOT NULL THEN
        RAISE EXCEPTION 'Teste exige banco vazio dedicado. Crie um banco novo.';
    END IF;
END
$guarda$;

\ir ../../docs/OLTP/diagrama_oltp.sql
\ir ../etl/instalar.sql

CREATE FUNCTION pg_temp.verificar(condicao BOOLEAN, descricao TEXT)
RETURNS VOID LANGUAGE plpgsql AS $teste$
BEGIN
    IF condicao IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'FALHOU: %', descricao;
    END IF;
    RAISE NOTICE 'PASSOU: %', descricao;
END
$teste$;

BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-01-01') AS carga_vazia;
COMMIT;
SELECT pg_temp.verificar((SELECT count(*) = 0 FROM dw_estrela.fato_venda), 'carga vazia sem fatos');
SELECT pg_temp.verificar((SELECT count(*) = 365 FROM dw_estrela.dim_tempo), 'calendário de um ano com origem vazia');

-- Dados sujos controlados simulam um OLTP legado com atributos descritivos opcionais.
ALTER TABLE public.medicamento ALTER COLUMN laboratorio DROP NOT NULL;
ALTER TABLE public.pessoa ALTER COLUMN cidade DROP NOT NULL;
ALTER TABLE public.item_venda ALTER COLUMN desconto DROP NOT NULL;
INSERT INTO public.unidade_farmacia
    (id, nome, cnpj, telefone, rua, numero, bairro, cidade, estado, cep)
VALUES (501, ' farmacia   centro ', '00000000000501', '8100000000', 'Rua A', '1',
        ' centro ', ' recife ', 'pe', '50000000');
INSERT INTO public.pessoa
    (id, nome, cpf, data_nascimento, telefone, email, rua, numero, bairro, cidade, estado, cep)
VALUES
    (101, '  ana   silva ', '00000000101', '1990-01-01', '8100000000', 'ana@test.invalid',
     'Rua A', '1', 'Centro', ' recife ', 'pe', '50000000'),
    (102, ' bruno ', '00000000102', '1980-01-01', '8100000000', 'bruno@test.invalid',
     'Rua B', '2', 'Centro', NULL, '', '50000000'),
    (103, ' luiza ', '00000000103', '1992-01-01', '8100000000', 'luiza@test.invalid',
     'Rua C', '3', 'Centro', 'Recife', 'PE', '50000000');
INSERT INTO public.cliente(id, numero_cartao_fidelidade) VALUES (101, 'C101'), (102, 'C102');
INSERT INTO public.funcionario(id, matricula, data_admissao, salario, cargo, unidade_id)
VALUES (103, 'F103', '2023-01-01', 2000, 'ATENDENTE', 501);
INSERT INTO public.departamento(id, nome) VALUES (601, ' medicamentos ');
INSERT INTO public.categoria(id, nome, departamento_id) VALUES (701, ' analgesicos ', 601);
INSERT INTO public.medicamento
    (id, nome, principio_ativo, codigo_barras, laboratorio, apresentacao, preco_base, categoria_id, tipo_controle)
VALUES
    (801, ' dipirona ', E'  dipirona\t sodica\n', 'M801', NULL, 'Caixa', 10, 701, 'ISENTO'),
    (802, ' vitamina c ', repeat('P', 120), 'M802', repeat('L', 120), 'Caixa', 5, 701, 'ISENTO');
INSERT INTO public.venda(id, data_hora, valor_total, status, cliente_id, unidade_id, funcionario_id)
VALUES
    (9001, '2024-02-29 10:00', 34, 'CONCLUIDA', 101, 501, 103),
    (9002, '2025-12-31 23:00', 10, 'CANCELADA', 102, 501, 103);
INSERT INTO public.item_venda
    (venda_id, numero_item, medicamento_id, quantidade, preco_unitario, desconto, valor_liquido)
VALUES
    (9001, 1, 801, 2, 10, 1, 999),
    (9001, 2, 802, 3, 5, NULL, 999),
    (9002, 1, 802, 2, 5, 0, 10);

BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-01-02') AS primeira_carga \gset
COMMIT;

SELECT pg_temp.verificar((SELECT count(*) = 3 FROM dw_estrela.fato_venda), 'granularidade de um item por venda');
SELECT pg_temp.verificar((SELECT sum(valor_liquido) = 34 FROM dw_estrela.vendas_concluidas), 'líquido derivado e canceladas fora do faturamento');
SELECT pg_temp.verificar((SELECT valor_bruto = 20 AND desconto = 1 AND valor_liquido = 19
    FROM dw_estrela.fato_venda WHERE venda_id_origem = 9001 AND numero_item = 1), 'quantidade vezes preço menos desconto absoluto');
SELECT pg_temp.verificar((SELECT nome = 'ANA SILVA' AND cidade = 'RECIFE' AND estado = 'PE'
    FROM dw_estrela.dim_cliente WHERE id_origem = 101 AND atual), 'padronização de nomes, espaços e UF');
SELECT pg_temp.verificar((SELECT cidade = 'NAO INFORMADO' AND estado = 'NI'
    FROM dw_estrela.dim_cliente WHERE id_origem = 102 AND atual), 'tratamento de nulos e UF desconhecida');
SELECT pg_temp.verificar((SELECT principio_ativo = 'DIPIRONA SODICA' AND fabricante = 'NAO INFORMADO'
    FROM dw_estrela.dim_produto WHERE id_origem = 801), 'espaços internos e fabricante nulo');
SELECT pg_temp.verificar((SELECT length(principio_ativo) = 120 AND length(fabricante) = 120
    FROM dw_estrela.dim_produto WHERE id_origem = 802), 'preservação da largura dos atributos do OLTP');
SELECT pg_temp.verificar((SELECT mes = 2 AND trimestre = 1 AND semestre = 1
    AND ano = 2024 AND dia_semana = 'QUINTA-FEIRA' FROM dw_estrela.dim_tempo WHERE data = '2024-02-29'),
    'dim_tempo inclui ano bissexto e atributos exigidos');
SELECT pg_temp.verificar((SELECT count(*) = 1096 FROM dw_estrela.dim_tempo), 'calendário contínuo e preservação dos anos carregados');
SELECT pg_temp.verificar((SELECT bool_and(sk_cliente <> id_origem AND sk_cliente > 0)
    FROM dw_estrela.dim_cliente WHERE id_origem > 0), 'chaves substitutas independentes da origem');
SELECT pg_temp.verificar((SELECT count(*) = 29 AND bool_and(depois IS NOT NULL)
    FROM etl.contagem WHERE execucao_id = :primeira_carga), 'COUNT antes e depois de origem, staging e modelos');
SELECT pg_temp.verificar((SELECT antes = 0 AND depois = 3 FROM etl.contagem
    WHERE execucao_id = :primeira_carga AND esquema = 'dw_estrela' AND tabela = 'fato_venda'),
    'contagem comprova a carga inicial');

-- Instalar novamente deve preservar fatos, dimensões, versões e auditoria.
\ir ../etl/instalar.sql
SELECT pg_temp.verificar((SELECT count(*) = 3 FROM dw_estrela.fato_venda), 'instalação repetida preserva os dados');

BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-01-03') AS repeticao \gset
COMMIT;
SELECT pg_temp.verificar((SELECT count(*) = 3 FROM dw_estrela.dim_cliente), 'reexecução sem duplicar versões');
SELECT pg_temp.verificar((SELECT count(*) = 3 AND bool_and(execucao_id = :primeira_carga)
    FROM dw_estrela.fato_venda), 'reexecução sem reinserir ou atualizar fatos inalterados');

UPDATE public.pessoa SET cidade = ' salvador ', estado = 'ba' WHERE id = 101;
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-02-01');
COMMIT;
SELECT pg_temp.verificar((SELECT count(*) = 2 AND count(*) FILTER (WHERE atual) = 1
    FROM dw_estrela.dim_cliente WHERE id_origem = 101), 'SCD2 encerra uma versão e abre outra');
SELECT pg_temp.verificar((SELECT data_fim = '2026-02-01'::timestamp AND NOT atual
    FROM dw_estrela.dim_cliente WHERE id_origem = 101 AND cidade = 'RECIFE'), 'fim da versão anterior');
SELECT pg_temp.verificar((SELECT c.cidade = 'RECIFE' FROM dw_estrela.fato_venda f
    JOIN dw_estrela.dim_cliente c USING (sk_cliente) WHERE venda_id_origem = 9001 AND numero_item = 1),
    'fatos antigos preservam a versão histórica');

UPDATE public.pessoa SET cidade = ' SALVADOR   ', estado = 'BA' WHERE id = 101;
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-02-02');
COMMIT;
SELECT pg_temp.verificar((SELECT count(*) = 2 FROM dw_estrela.dim_cliente WHERE id_origem = 101),
    'variação de caixa e espaços evita falsa mudança SCD');

INSERT INTO public.venda(id, data_hora, valor_total, status, cliente_id, unidade_id, funcionario_id)
VALUES
    (9003, '2026-02-01 00:00', 18, 'CONCLUIDA', 101, 501, 103),
    (9004, '2024-06-01 10:00', 5, 'CONCLUIDA', 101, 501, 103);
INSERT INTO public.item_venda
    (venda_id, numero_item, medicamento_id, quantidade, preco_unitario, desconto, valor_liquido)
VALUES (9003, 1, 801, 2, 10, 2, 18), (9004, 1, 802, 1, 5, 0, 5);
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-02-03');
COMMIT;
SELECT pg_temp.verificar((SELECT c.cidade = 'SALVADOR' FROM dw_estrela.fato_venda f
    JOIN dw_estrela.dim_cliente c USING (sk_cliente) WHERE venda_id_origem = 9003),
    'venda no limite inicial usa a nova versão SCD');
SELECT pg_temp.verificar((SELECT c.cidade = 'RECIFE' FROM dw_estrela.fato_venda f
    JOIN dw_estrela.dim_cliente c USING (sk_cliente) WHERE venda_id_origem = 9004),
    'venda retroativa usa a versão histórica correta');

UPDATE public.venda SET status = 'CANCELADA' WHERE id = 9001;
UPDATE public.item_venda SET quantidade = 3, valor_liquido = 28 WHERE venda_id = 9003;
DELETE FROM public.item_venda WHERE venda_id = 9004;
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-02-04');
COMMIT;
SELECT pg_temp.verificar((SELECT sum(valor_liquido) = 28 FROM dw_estrela.vendas_concluidas),
    'incremental atualiza medidas, cancelamentos e exclusões');
SELECT pg_temp.verificar((SELECT count(*) = 5 AND count(*) FILTER (WHERE excluido_origem) = 1
    FROM dw_estrela.fato_venda), 'exclusão da origem mantém histórico na fato');

INSERT INTO public.item_venda
    (venda_id, numero_item, medicamento_id, quantidade, preco_unitario, desconto, valor_liquido)
VALUES (9004, 1, 802, 1, 5, 0, 5);
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-02-05');
COMMIT;
SELECT pg_temp.verificar((SELECT count(*) = 5 AND NOT bool_or(excluido_origem)
    FROM dw_estrela.fato_venda), 'reaparecimento da origem reutiliza a fato existente');

UPDATE public.pessoa SET cidade = 'Recife', estado = 'PE' WHERE id = 101;
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-03-01');
COMMIT;
SELECT pg_temp.verificar((SELECT count(*) = 3 AND count(*) FILTER (WHERE atual) = 1
    FROM dw_estrela.dim_cliente WHERE id_origem = 101), 'retorno à cidade anterior cria terceira versão');

-- Falha no segundo modelo deve desfazer inclusive a atualização já aplicada ao primeiro.
ALTER TABLE dw_floco_neve.fato_venda ADD CONSTRAINT teste_rejeita_dezessete CHECK (valor_liquido <> 17);
UPDATE public.item_venda SET preco_unitario = 6, desconto = 1 WHERE venda_id = 9003;
BEGIN ISOLATION LEVEL REPEATABLE READ;
DO $rollback$
DECLARE
    execucoes BIGINT;
BEGIN
    SELECT count(*) INTO execucoes FROM etl.execucao;
    BEGIN
        PERFORM etl.executar_carga('2026-03-02');
        RAISE EXCEPTION 'A carga deveria ter falhado';
    EXCEPTION WHEN check_violation THEN
        PERFORM pg_temp.verificar(TRUE, 'falha controlada no segundo modelo');
    END;
    PERFORM pg_temp.verificar((SELECT valor_liquido = 28 FROM dw_estrela.fato_venda
        WHERE venda_id_origem = 9003), 'rollback atômico nos dois modelos');
    PERFORM pg_temp.verificar((SELECT count(*) = execucoes FROM etl.execucao),
        'falha evita registro incorreto de sucesso');
END
$rollback$;
COMMIT;
ALTER TABLE dw_floco_neve.fato_venda DROP CONSTRAINT teste_rejeita_dezessete;
UPDATE public.item_venda SET preco_unitario = 10, desconto = 2 WHERE venda_id = 9003;

-- Uma origem com desconto maior que o bruto deve interromper a carga inteira.
UPDATE public.item_venda SET desconto = 31 WHERE venda_id = 9003;
BEGIN ISOLATION LEVEL REPEATABLE READ;
DO $invalido$
BEGIN
    BEGIN
        PERFORM etl.executar_carga('2026-03-02');
        RAISE EXCEPTION 'A carga deveria rejeitar o desconto';
    EXCEPTION WHEN check_violation THEN
        PERFORM pg_temp.verificar(TRUE, 'desconto inválido é rejeitado');
    END;
    PERFORM pg_temp.verificar((SELECT valor_liquido = 28 FROM dw_estrela.fato_venda
        WHERE venda_id_origem = 9003), 'origem inválida preserva o DW anterior');
END
$invalido$;
COMMIT;
UPDATE public.item_venda SET desconto = 2 WHERE venda_id = 9003;

-- Em um legado sem FK, referências ausentes resolvem para o membro desconhecido.
ALTER TABLE public.item_venda DROP CONSTRAINT item_venda_medicamento_id_fkey;
INSERT INTO public.item_venda
    (venda_id, numero_item, medicamento_id, quantidade, preco_unitario, desconto, valor_liquido)
VALUES (9004, 2, 999999, 1, 2, 0, 2);
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga('2026-03-02');
COMMIT;
SELECT pg_temp.verificar((SELECT sk_produto = 0 FROM dw_estrela.fato_venda
    WHERE venda_id_origem = 9004 AND numero_item = 2), 'referência ausente recebe SK zero');

SELECT pg_temp.verificar((SELECT count(*) = 0 FROM dw_estrela.fato_venda f
    LEFT JOIN dw_estrela.dim_produto p USING (sk_produto)
    LEFT JOIN dw_estrela.dim_cliente c USING (sk_cliente)
    LEFT JOIN dw_estrela.dim_loja l USING (sk_loja)
    LEFT JOIN dw_estrela.dim_tempo t USING (sk_tempo)
    WHERE p.sk_produto IS NULL OR c.sk_cliente IS NULL OR l.sk_loja IS NULL OR t.sk_tempo IS NULL),
    'fatos possuem todas as dimensões resolvidas');
SELECT pg_temp.verificar((SELECT sum(f.valor_liquido) = 35 FROM dw_floco_neve.vendas_concluidas f
    JOIN dw_floco_neve.dim_produto p USING (sk_produto)
    JOIN dw_floco_neve.dim_categoria c USING (sk_categoria)
    JOIN dw_floco_neve.dim_departamento d USING (sk_departamento)
    JOIN dw_floco_neve.dim_loja l USING (sk_loja)
    JOIN dw_floco_neve.dim_cidade ci USING (sk_cidade)
    JOIN dw_floco_neve.dim_estado e USING (sk_estado)), 'hierarquias completas no floco de neve');
SELECT pg_temp.verificar(NOT EXISTS (
    (SELECT venda_id_origem, numero_item, quantidade, valor_bruto, desconto, valor_liquido, status_venda, excluido_origem
     FROM dw_estrela.fato_venda EXCEPT
     SELECT venda_id_origem, numero_item, quantidade, valor_bruto, desconto, valor_liquido, status_venda, excluido_origem
     FROM dw_floco_neve.fato_venda)
    UNION ALL
    (SELECT venda_id_origem, numero_item, quantidade, valor_bruto, desconto, valor_liquido, status_venda, excluido_origem
     FROM dw_floco_neve.fato_venda EXCEPT
     SELECT venda_id_origem, numero_item, quantidade, valor_bruto, desconto, valor_liquido, status_venda, excluido_origem
     FROM dw_estrela.fato_venda)
), 'fatos dos dois modelos coincidem item a item');
SELECT pg_temp.verificar((SELECT sum(valor_liquido) FROM dw_estrela.vendas_concluidas) =
    (SELECT sum(i.quantidade * i.preco_unitario - COALESCE(i.desconto, 0))
     FROM public.item_venda i JOIN public.venda v ON v.id = i.venda_id WHERE v.status = 'CONCLUIDA'),
    'reconciliação financeira entre OLTP e DW');

-- Uma fato legada exige reconciliação explícita da chave de origem.
INSERT INTO dw_estrela.fato_venda
    (sk_tempo, sk_produto, sk_cliente, sk_loja, quantidade, valor_bruto, desconto, valor_liquido)
VALUES (20260101, 0, 0, 0, 1, 1, 0, 1);
BEGIN ISOLATION LEVEL REPEATABLE READ;
DO $legado$
BEGIN
    BEGIN
        PERFORM etl.executar_carga('2026-03-03');
        RAISE EXCEPTION 'A fato legada deveria impedir a carga';
    EXCEPTION WHEN raise_exception THEN
        PERFORM pg_temp.verificar(SQLERRM LIKE 'Há fatos sem identificação de origem%',
            'fatos legados sem rastreabilidade são detectados');
    END;
END
$legado$;
COMMIT;
DELETE FROM dw_estrela.fato_venda WHERE venda_id_origem IS NULL;

-- A reinstalação também deve preservar o histórico SCD2 já construído.
\ir ../etl/instalar.sql
SELECT pg_temp.verificar((SELECT count(*) = 3 FROM dw_estrela.dim_cliente WHERE id_origem = 101),
    'reinstalação preserva todas as versões SCD2');

DO $isolamento$
BEGIN
    BEGIN
        PERFORM etl.executar_carga('2026-03-03');
        RAISE EXCEPTION 'O isolamento deveria ser rejeitado';
    EXCEPTION WHEN raise_exception THEN
        PERFORM pg_temp.verificar(SQLERRM LIKE 'Execute a carga dentro de BEGIN%',
            'fotografia consistente exige isolamento adequado');
    END;
END
$isolamento$;

BEGIN ISOLATION LEVEL REPEATABLE READ;
DO $cronologia$
BEGIN
    BEGIN
        PERFORM etl.executar_carga('2026-03-02');
        RAISE EXCEPTION 'A referência deveria ser rejeitada';
    EXCEPTION WHEN raise_exception THEN
        PERFORM pg_temp.verificar(SQLERRM LIKE 'A referência deve ser posterior%',
            'referência repetida é rejeitada');
    END;
END
$cronologia$;
COMMIT;

SELECT e.id, e.referencia, c.esquema, c.tabela, c.antes, c.depois
FROM etl.execucao e JOIN etl.contagem c ON c.execucao_id = e.id
WHERE c.tabela IN ('fato_venda', 'dim_cliente')
ORDER BY e.id, c.esquema, c.tabela;
