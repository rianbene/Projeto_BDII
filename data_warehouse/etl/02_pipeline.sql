CREATE OR REPLACE FUNCTION etl.normalizar_texto(valor TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $sql$
    SELECT COALESCE(NULLIF(upper(btrim(regexp_replace(valor, '[[:space:]]+', ' ', 'g'))), ''),
                    'NAO INFORMADO')
$sql$;

CREATE OR REPLACE FUNCTION etl.normalizar_uf(valor TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $sql$
    SELECT CASE WHEN etl.normalizar_texto(valor) IN
        ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
         'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')
        THEN etl.normalizar_texto(valor) ELSE 'NI' END
$sql$;

CREATE OR REPLACE FUNCTION etl.faixa_etaria(nascimento DATE, referencia DATE)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $sql$
    SELECT CASE
        WHEN nascimento IS NULL OR nascimento > referencia THEN 'NAO INFORMADA'
        WHEN extract(year FROM age(referencia, nascimento)) < 18 THEN '0 A 17'
        WHEN extract(year FROM age(referencia, nascimento)) < 30 THEN '18 A 29'
        WHEN extract(year FROM age(referencia, nascimento)) < 45 THEN '30 A 44'
        WHEN extract(year FROM age(referencia, nascimento)) < 60 THEN '45 A 59'
        ELSE '60 OU MAIS'
    END
$sql$;

CREATE OR REPLACE FUNCTION etl.registrar_contagens(carga BIGINT, apos BOOLEAN)
RETURNS VOID LANGUAGE plpgsql AS $contagens$
DECLARE
    objeto RECORD;
    linhas BIGINT;
BEGIN
    FOR objeto IN
        SELECT table_schema, table_name FROM information_schema.tables
        WHERE table_type = 'BASE TABLE'
          AND (table_schema IN ('dw_estrela', 'dw_floco_neve', 'staging')
               OR (table_schema = 'public' AND table_name IN
                   ('pessoa','cliente','funcionario','unidade_farmacia','departamento',
                    'categoria','medicamento','venda','item_venda','pagamento')))
        ORDER BY table_schema, table_name
    LOOP
        EXECUTE format('SELECT count(*) FROM %I.%I', objeto.table_schema, objeto.table_name) INTO linhas;
        IF apos THEN
            UPDATE etl.contagem SET depois = linhas
            WHERE execucao_id = carga AND esquema = objeto.table_schema AND tabela = objeto.table_name;
        ELSE
            INSERT INTO etl.contagem(execucao_id, esquema, tabela, antes)
            VALUES (carga, objeto.table_schema, objeto.table_name, linhas);
        END IF;
    END LOOP;
END
$contagens$;

CREATE OR REPLACE FUNCTION etl.carregar_modelo(modelo TEXT, carga BIGINT, referencia TIMESTAMP)
RETURNS VOID LANGUAGE plpgsql AS $modelo$
BEGIN
    IF modelo NOT IN ('dw_estrela', 'dw_floco_neve') OR modelo IS NULL THEN
        RAISE EXCEPTION 'Modelo dimensional desconhecido: %', modelo;
    END IF;

    -- SK zero representa uma referência ausente. Os SERIAL continuam gerando a partir de um.
    IF modelo = 'dw_floco_neve' THEN
        INSERT INTO dw_floco_neve.dim_departamento(sk_departamento, nome)
        VALUES (0, 'NAO INFORMADO') ON CONFLICT DO NOTHING;
        INSERT INTO dw_floco_neve.dim_categoria(sk_categoria, nome, sk_departamento)
        VALUES (0, 'NAO INFORMADO', 0) ON CONFLICT DO NOTHING;
        INSERT INTO dw_floco_neve.dim_estado(sk_estado, nome, sigla)
        VALUES (0, 'NAO INFORMADO', 'NI') ON CONFLICT DO NOTHING;
        INSERT INTO dw_floco_neve.dim_cidade(sk_cidade, nome, sk_estado)
        VALUES (0, 'NAO INFORMADO', 0) ON CONFLICT DO NOTHING;

        INSERT INTO dw_floco_neve.dim_departamento(nome)
        SELECT DISTINCT departamento FROM staging.produto
        ON CONFLICT (nome) DO NOTHING;

        INSERT INTO dw_floco_neve.dim_categoria(nome, sk_departamento)
        SELECT DISTINCT p.categoria, d.sk_departamento
        FROM staging.produto p JOIN dw_floco_neve.dim_departamento d ON d.nome = p.departamento
        ON CONFLICT (nome, sk_departamento) DO NOTHING;

        INSERT INTO dw_floco_neve.dim_estado(nome, sigla)
        SELECT DISTINCT COALESCE(uf.nome, 'NAO INFORMADO'), l.estado
        FROM staging.loja l
        LEFT JOIN (VALUES
            ('AC','ACRE'), ('AL','ALAGOAS'), ('AP','AMAPA'), ('AM','AMAZONAS'),
            ('BA','BAHIA'), ('CE','CEARA'), ('DF','DISTRITO FEDERAL'), ('ES','ESPIRITO SANTO'),
            ('GO','GOIAS'), ('MA','MARANHAO'), ('MT','MATO GROSSO'), ('MS','MATO GROSSO DO SUL'),
            ('MG','MINAS GERAIS'), ('PA','PARA'), ('PB','PARAIBA'), ('PR','PARANA'),
            ('PE','PERNAMBUCO'), ('PI','PIAUI'), ('RJ','RIO DE JANEIRO'), ('RN','RIO GRANDE DO NORTE'),
            ('RS','RIO GRANDE DO SUL'), ('RO','RONDONIA'), ('RR','RORAIMA'), ('SC','SANTA CATARINA'),
            ('SP','SAO PAULO'), ('SE','SERGIPE'), ('TO','TOCANTINS')
        ) uf(sigla, nome) ON uf.sigla = l.estado
        ON CONFLICT (sigla) DO NOTHING;

        INSERT INTO dw_floco_neve.dim_cidade(nome, sk_estado)
        SELECT DISTINCT l.cidade, e.sk_estado
        FROM staging.loja l JOIN dw_floco_neve.dim_estado e ON e.sigla = l.estado
        ON CONFLICT (nome, sk_estado) DO NOTHING;

        INSERT INTO dw_floco_neve.dim_produto
            (sk_produto, id_origem, nome, principio_ativo, sk_categoria)
        VALUES (0, -1, 'NAO INFORMADO', 'NAO INFORMADO', 0)
        ON CONFLICT (id_origem) DO NOTHING;

        INSERT INTO dw_floco_neve.dim_produto AS destino
            (id_origem, nome, principio_ativo, sk_categoria)
        SELECT p.id_origem, p.nome, p.principio_ativo, c.sk_categoria
        FROM staging.produto p
        JOIN dw_floco_neve.dim_departamento d ON d.nome = p.departamento
        JOIN dw_floco_neve.dim_categoria c ON c.nome = p.categoria AND c.sk_departamento = d.sk_departamento
        ON CONFLICT (id_origem) DO UPDATE
        SET nome = EXCLUDED.nome, principio_ativo = EXCLUDED.principio_ativo,
            sk_categoria = EXCLUDED.sk_categoria
        WHERE (destino.nome, destino.principio_ativo, destino.sk_categoria)
            IS DISTINCT FROM (EXCLUDED.nome, EXCLUDED.principio_ativo, EXCLUDED.sk_categoria);

        INSERT INTO dw_floco_neve.dim_loja
            (sk_loja, id_origem, nome_fantasia, bairro, sk_cidade)
        VALUES (0, -1, 'NAO INFORMADO', 'NAO INFORMADO', 0)
        ON CONFLICT (id_origem) DO NOTHING;

        INSERT INTO dw_floco_neve.dim_loja AS destino(id_origem, nome_fantasia, bairro, sk_cidade)
        SELECT l.id_origem, l.nome_fantasia, l.bairro, c.sk_cidade
        FROM staging.loja l
        JOIN dw_floco_neve.dim_estado e ON e.sigla = l.estado
        JOIN dw_floco_neve.dim_cidade c ON c.nome = l.cidade AND c.sk_estado = e.sk_estado
        ON CONFLICT (id_origem) DO UPDATE
        SET nome_fantasia = EXCLUDED.nome_fantasia, bairro = EXCLUDED.bairro, sk_cidade = EXCLUDED.sk_cidade
        WHERE (destino.nome_fantasia, destino.bairro, destino.sk_cidade)
            IS DISTINCT FROM (EXCLUDED.nome_fantasia, EXCLUDED.bairro, EXCLUDED.sk_cidade);
    ELSE
        INSERT INTO dw_estrela.dim_produto
            (sk_produto, id_origem, nome, principio_ativo, categoria, fabricante)
        VALUES (0, -1, 'NAO INFORMADO', 'NAO INFORMADO', 'NAO INFORMADO', 'NAO INFORMADO')
        ON CONFLICT (id_origem) DO NOTHING;

        INSERT INTO dw_estrela.dim_produto AS destino
            (id_origem, nome, principio_ativo, categoria, fabricante)
        SELECT id_origem, nome, principio_ativo, categoria, fabricante FROM staging.produto
        ON CONFLICT (id_origem) DO UPDATE
        SET nome = EXCLUDED.nome, principio_ativo = EXCLUDED.principio_ativo,
            categoria = EXCLUDED.categoria, fabricante = EXCLUDED.fabricante
        WHERE (destino.nome, destino.principio_ativo, destino.categoria, destino.fabricante)
            IS DISTINCT FROM (EXCLUDED.nome, EXCLUDED.principio_ativo, EXCLUDED.categoria, EXCLUDED.fabricante);

        INSERT INTO dw_estrela.dim_loja
            (sk_loja, id_origem, nome_fantasia, bairro, cidade, estado)
        VALUES (0, -1, 'NAO INFORMADO', 'NAO INFORMADO', 'NAO INFORMADO', 'NI')
        ON CONFLICT (id_origem) DO NOTHING;

        INSERT INTO dw_estrela.dim_loja AS destino(id_origem, nome_fantasia, bairro, cidade, estado)
        SELECT id_origem, nome_fantasia, bairro, cidade, estado FROM staging.loja
        ON CONFLICT (id_origem) DO UPDATE
        SET nome_fantasia = EXCLUDED.nome_fantasia, bairro = EXCLUDED.bairro,
            cidade = EXCLUDED.cidade, estado = EXCLUDED.estado
        WHERE (destino.nome_fantasia, destino.bairro, destino.cidade, destino.estado)
            IS DISTINCT FROM (EXCLUDED.nome_fantasia, EXCLUDED.bairro, EXCLUDED.cidade, EXCLUDED.estado);
    END IF;

    EXECUTE format('
        INSERT INTO %I.dim_tempo
            (sk_tempo, data, dia, mes, nome_mes, trimestre, semestre, ano, dia_semana)
        SELECT sk_tempo, data, dia, mes, nome_mes, trimestre, semestre, ano, dia_semana
        FROM staging.tempo ON CONFLICT (sk_tempo) DO NOTHING', modelo);

    EXECUTE format('
        INSERT INTO %I.dim_cliente
            (sk_cliente, id_origem, nome, cidade, estado, faixa_etaria)
        VALUES (0, -1, ''NAO INFORMADO'', ''NAO INFORMADO'', ''NI'', ''NAO INFORMADA'')
        ON CONFLICT (id_origem) WHERE atual DO NOTHING', modelo);

    -- SCD2 fecha apenas as versões cujos atributos normalizados realmente mudaram.
    EXECUTE format('
        UPDATE %I.dim_cliente d SET data_fim = $1, atual = FALSE
        FROM staging.cliente s
        WHERE d.id_origem = s.id_origem AND d.atual
          AND (d.nome, d.cidade, d.estado, d.data_nascimento, d.faixa_etaria)
              IS DISTINCT FROM (s.nome, s.cidade, s.estado, s.data_nascimento, s.faixa_etaria)', modelo)
        USING referencia;

    EXECUTE format('
        INSERT INTO %1$I.dim_cliente
            (id_origem, nome, cidade, estado, data_nascimento, faixa_etaria, data_inicio)
        SELECT s.id_origem, s.nome, s.cidade, s.estado, s.data_nascimento, s.faixa_etaria,
               CASE WHEN EXISTS (SELECT 1 FROM %1$I.dim_cliente h WHERE h.id_origem = s.id_origem)
                    THEN $1 ELSE ''-infinity''::timestamp END
        FROM staging.cliente s
        WHERE NOT EXISTS (
            SELECT 1 FROM %1$I.dim_cliente d WHERE d.id_origem = s.id_origem AND d.atual)', modelo)
        USING referencia;

    -- O horário da venda seleciona a versão do cliente válida para cada item, inclusive atrasado.
    EXECUTE format('
        INSERT INTO %1$I.fato_venda AS destino
            (venda_id_origem, numero_item, sk_tempo, sk_produto, sk_cliente, sk_loja,
             quantidade, valor_bruto, desconto, valor_liquido, status_venda, execucao_id)
        SELECT s.venda_id_origem, s.numero_item, t.sk_tempo,
               COALESCE(p.sk_produto, 0), COALESCE(c.sk_cliente, 0), COALESCE(l.sk_loja, 0),
               s.quantidade, s.valor_bruto, s.desconto, s.valor_liquido, s.status_venda, $1
        FROM staging.item_venda s
        JOIN %1$I.dim_tempo t ON t.data = s.data_hora::date
        LEFT JOIN %1$I.dim_produto p ON p.id_origem = s.produto_id
        LEFT JOIN %1$I.dim_loja l ON l.id_origem = s.loja_id
        LEFT JOIN %1$I.dim_cliente c ON c.id_origem = s.cliente_id
            AND s.data_hora >= c.data_inicio AND (c.data_fim IS NULL OR s.data_hora < c.data_fim)
        ON CONFLICT (venda_id_origem, numero_item) DO UPDATE
        SET sk_tempo = EXCLUDED.sk_tempo, sk_produto = EXCLUDED.sk_produto,
            sk_cliente = EXCLUDED.sk_cliente, sk_loja = EXCLUDED.sk_loja,
            quantidade = EXCLUDED.quantidade, valor_bruto = EXCLUDED.valor_bruto,
            desconto = EXCLUDED.desconto, valor_liquido = EXCLUDED.valor_liquido,
            status_venda = EXCLUDED.status_venda, excluido_origem = FALSE, execucao_id = EXCLUDED.execucao_id
        WHERE (destino.sk_tempo, destino.sk_produto, destino.sk_cliente, destino.sk_loja,
               destino.quantidade, destino.valor_bruto, destino.desconto, destino.valor_liquido,
               destino.status_venda, destino.excluido_origem)
            IS DISTINCT FROM
              (EXCLUDED.sk_tempo, EXCLUDED.sk_produto, EXCLUDED.sk_cliente, EXCLUDED.sk_loja,
               EXCLUDED.quantidade, EXCLUDED.valor_bruto, EXCLUDED.desconto, EXCLUDED.valor_liquido,
               EXCLUDED.status_venda, FALSE)', modelo)
        USING carga;

    EXECUTE format('
        UPDATE %I.fato_venda f SET excluido_origem = TRUE, execucao_id = $1
        WHERE NOT f.excluido_origem AND NOT EXISTS (
            SELECT 1 FROM staging.item_venda s
            WHERE s.venda_id_origem = f.venda_id_origem AND s.numero_item = f.numero_item)', modelo)
        USING carga;
END
$modelo$;

CREATE OR REPLACE FUNCTION etl.executar_carga(referencia TIMESTAMP DEFAULT localtimestamp)
RETURNS BIGINT LANGUAGE plpgsql AS $carga$
DECLARE
    carga_id BIGINT;
    modelo TEXT;
    sem_origem BOOLEAN;
    inicio_calendario DATE;
    fim_calendario DATE;
    itens_origem BIGINT;
    itens_destino BIGINT;
    liquido_origem NUMERIC;
    liquido_destino NUMERIC;
BEGIN
    IF current_setting('transaction_isolation') NOT IN ('repeatable read', 'serializable') THEN
        RAISE EXCEPTION 'Execute a carga dentro de BEGIN ISOLATION LEVEL REPEATABLE READ';
    END IF;
    IF NOT pg_try_advisory_xact_lock(20261122, 2) THEN
        RAISE EXCEPTION 'Outra instalação ou carga ETL está em execução. Tente novamente após o término.';
    END IF;
    IF referencia IS NULL OR NOT isfinite(referencia) THEN
        RAISE EXCEPTION 'Informe uma referência de carga finita';
    END IF;
    IF EXISTS (SELECT 1 FROM etl.execucao e WHERE e.referencia >= executar_carga.referencia) THEN
        RAISE EXCEPTION 'A referência deve ser posterior à última carga concluída';
    END IF;

    FOREACH modelo IN ARRAY ARRAY['dw_estrela', 'dw_floco_neve'] LOOP
        EXECUTE format('
            SELECT EXISTS (SELECT 1 FROM %I.fato_venda
                           WHERE venda_id_origem IS NULL OR numero_item IS NULL)', modelo)
            INTO sem_origem;
        IF sem_origem THEN
            RAISE EXCEPTION 'Há fatos sem identificação de origem em %. Reconcilie venda_id_origem e numero_item antes da carga.', modelo;
        END IF;
    END LOOP;

    INSERT INTO etl.execucao(referencia) VALUES (referencia) RETURNING id INTO carga_id;
    PERFORM etl.registrar_contagens(carga_id, FALSE);

    TRUNCATE staging.cliente, staging.produto, staging.loja, staging.tempo, staging.item_venda;

    INSERT INTO staging.cliente
    SELECT c.id, etl.normalizar_texto(p.nome), etl.normalizar_texto(p.cidade),
           etl.normalizar_uf(p.estado), p.data_nascimento,
           etl.faixa_etaria(p.data_nascimento, referencia::date)
    FROM public.cliente c LEFT JOIN public.pessoa p ON p.id = c.id;

    INSERT INTO staging.produto
    SELECT m.id, etl.normalizar_texto(m.nome), etl.normalizar_texto(m.principio_ativo),
           etl.normalizar_texto(c.nome), etl.normalizar_texto(d.nome), etl.normalizar_texto(m.laboratorio)
    FROM public.medicamento m
    LEFT JOIN public.categoria c ON c.id = m.categoria_id
    LEFT JOIN public.departamento d ON d.id = c.departamento_id;

    INSERT INTO staging.loja
    SELECT id, etl.normalizar_texto(nome), etl.normalizar_texto(bairro),
           etl.normalizar_texto(cidade), etl.normalizar_uf(estado)
    FROM public.unidade_farmacia;

    IF EXISTS (SELECT 1 FROM staging.cliente WHERE id_origem <= 0)
       OR EXISTS (SELECT 1 FROM staging.produto WHERE id_origem <= 0)
       OR EXISTS (SELECT 1 FROM staging.loja WHERE id_origem <= 0) THEN
        RAISE EXCEPTION 'IDs de origem devem ser positivos. A chave zero é reservada ao membro desconhecido.';
    END IF;

    INSERT INTO staging.item_venda
    SELECT v.id, i.numero_item, v.data_hora, v.cliente_id, i.medicamento_id, v.unidade_id,
           upper(btrim(v.status)), i.quantidade, i.quantidade * i.preco_unitario,
           COALESCE(i.desconto, 0),
           i.quantidade * i.preco_unitario - COALESCE(i.desconto, 0)
    FROM public.venda v JOIN public.item_venda i ON i.venda_id = v.id;

    SELECT date_trunc('year', COALESCE(min(data_hora), referencia))::date,
           (date_trunc('year', COALESCE(max(data_hora), referencia)) + interval '1 year - 1 day')::date
    INTO inicio_calendario, fim_calendario FROM staging.item_venda;

    INSERT INTO staging.tempo
    SELECT extract(year FROM dia)::int * 10000 + extract(month FROM dia)::int * 100 + extract(day FROM dia)::int,
           dia::date, extract(day FROM dia)::smallint, extract(month FROM dia)::smallint,
           (ARRAY['JANEIRO','FEVEREIRO','MARCO','ABRIL','MAIO','JUNHO',
                  'JULHO','AGOSTO','SETEMBRO','OUTUBRO','NOVEMBRO','DEZEMBRO'])[extract(month FROM dia)::int],
           extract(quarter FROM dia)::smallint,
           ((extract(month FROM dia)::int - 1) / 6 + 1)::smallint,
           extract(year FROM dia)::smallint,
           (ARRAY['DOMINGO','SEGUNDA-FEIRA','TERCA-FEIRA','QUARTA-FEIRA',
                  'QUINTA-FEIRA','SEXTA-FEIRA','SABADO'])[extract(dow FROM dia)::int + 1]
    FROM generate_series(inicio_calendario::timestamp, fim_calendario::timestamp, interval '1 day') AS dias(dia);

    SELECT count(*), COALESCE(sum(valor_liquido) FILTER (WHERE status_venda = 'CONCLUIDA'), 0)
    INTO itens_origem, liquido_origem FROM staging.item_venda;
    FOREACH modelo IN ARRAY ARRAY['dw_estrela', 'dw_floco_neve'] LOOP
        PERFORM etl.carregar_modelo(modelo, carga_id, referencia);
        EXECUTE format('
            SELECT count(*),
                   COALESCE(sum(valor_liquido) FILTER (WHERE status_venda = ''CONCLUIDA''), 0)
            FROM %I.fato_venda WHERE NOT excluido_origem', modelo)
            INTO itens_destino, liquido_destino;
        IF itens_destino <> itens_origem OR liquido_destino <> liquido_origem THEN
            RAISE EXCEPTION 'Reconciliação falhou no modelo %: itens %/%, líquido %/%',
                modelo, itens_origem, itens_destino, liquido_origem, liquido_destino;
        END IF;
    END LOOP;
    PERFORM etl.registrar_contagens(carga_id, TRUE);
    UPDATE etl.execucao SET status = 'CONCLUIDA', concluido_em = clock_timestamp() WHERE id = carga_id;
    RETURN carga_id;
END
$carga$;
