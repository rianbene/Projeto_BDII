CREATE SCHEMA IF NOT EXISTS etl;
CREATE SCHEMA IF NOT EXISTS staging;

CREATE TABLE IF NOT EXISTS etl.execucao (
    id BIGSERIAL PRIMARY KEY,
    iniciado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    concluido_em TIMESTAMPTZ,
    referencia TIMESTAMP NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'EM_EXECUCAO'
        CHECK (status IN ('EM_EXECUCAO', 'CONCLUIDA'))
);

CREATE TABLE IF NOT EXISTS etl.contagem (
    execucao_id BIGINT NOT NULL REFERENCES etl.execucao(id),
    esquema TEXT NOT NULL,
    tabela TEXT NOT NULL,
    antes BIGINT NOT NULL,
    depois BIGINT,
    PRIMARY KEY (execucao_id, esquema, tabela)
);

CREATE TABLE IF NOT EXISTS staging.cliente (
    id_origem BIGINT PRIMARY KEY,
    nome TEXT NOT NULL,
    cidade TEXT NOT NULL,
    estado CHAR(2) NOT NULL,
    data_nascimento DATE,
    faixa_etaria VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS staging.produto (
    id_origem BIGINT PRIMARY KEY,
    nome TEXT NOT NULL,
    principio_ativo TEXT NOT NULL,
    categoria TEXT NOT NULL,
    departamento TEXT NOT NULL,
    fabricante TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS staging.loja (
    id_origem BIGINT PRIMARY KEY,
    nome_fantasia TEXT NOT NULL,
    bairro TEXT NOT NULL,
    cidade TEXT NOT NULL,
    estado CHAR(2) NOT NULL
);

CREATE TABLE IF NOT EXISTS staging.tempo (
    sk_tempo INT PRIMARY KEY,
    data DATE NOT NULL UNIQUE,
    dia SMALLINT NOT NULL,
    mes SMALLINT NOT NULL,
    nome_mes VARCHAR(15) NOT NULL,
    trimestre SMALLINT NOT NULL,
    semestre SMALLINT NOT NULL,
    ano SMALLINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS staging.item_venda (
    venda_id_origem BIGINT NOT NULL,
    numero_item INT NOT NULL CHECK (numero_item > 0),
    data_hora TIMESTAMP NOT NULL,
    cliente_id BIGINT,
    produto_id BIGINT,
    loja_id BIGINT,
    status_venda VARCHAR(20) NOT NULL CHECK (status_venda IN ('CONCLUIDA', 'CANCELADA')),
    quantidade INT NOT NULL CHECK (quantidade > 0),
    valor_bruto NUMERIC NOT NULL CHECK (valor_bruto >= 0),
    desconto NUMERIC NOT NULL CHECK (desconto >= 0 AND desconto <= valor_bruto),
    valor_liquido NUMERIC NOT NULL CHECK (valor_liquido = valor_bruto - desconto),
    PRIMARY KEY (venda_id_origem, numero_item)
);

-- Amplia os atributos cuja origem admite mais caracteres que o DDL dimensional.
ALTER TABLE dw_estrela.dim_produto
    ALTER COLUMN principio_ativo TYPE VARCHAR(120),
    ALTER COLUMN categoria TYPE VARCHAR(80),
    ALTER COLUMN fabricante TYPE VARCHAR(120);
ALTER TABLE dw_floco_neve.dim_produto ALTER COLUMN principio_ativo TYPE VARCHAR(120);
ALTER TABLE dw_floco_neve.dim_departamento ALTER COLUMN nome TYPE VARCHAR(80);
ALTER TABLE dw_floco_neve.dim_categoria ALTER COLUMN nome TYPE VARCHAR(80);

DO $migracao$
DECLARE
    modelo TEXT;
BEGIN
    FOREACH modelo IN ARRAY ARRAY['dw_estrela', 'dw_floco_neve'] LOOP
        EXECUTE format('
            ALTER TABLE %I.dim_loja ALTER COLUMN nome_fantasia TYPE VARCHAR(120)', modelo);
        EXECUTE format('
            ALTER TABLE %I.dim_cliente
                ADD COLUMN IF NOT EXISTS data_inicio TIMESTAMP NOT NULL DEFAULT ''-infinity'',
                ADD COLUMN IF NOT EXISTS data_fim TIMESTAMP,
                ADD COLUMN IF NOT EXISTS atual BOOLEAN NOT NULL DEFAULT TRUE', modelo);
        EXECUTE format('
            CREATE UNIQUE INDEX IF NOT EXISTS uq_cliente_versao
            ON %I.dim_cliente (id_origem, data_inicio)', modelo);
        EXECUTE format('
            CREATE UNIQUE INDEX IF NOT EXISTS uq_cliente_atual
            ON %I.dim_cliente (id_origem) WHERE atual', modelo);
        EXECUTE format('
            CREATE UNIQUE INDEX IF NOT EXISTS uq_produto_origem
            ON %I.dim_produto (id_origem)', modelo);
        EXECUTE format('
            CREATE UNIQUE INDEX IF NOT EXISTS uq_loja_origem
            ON %I.dim_loja (id_origem)', modelo);
        EXECUTE format('
            ALTER TABLE %I.fato_venda
                ADD COLUMN IF NOT EXISTS venda_id_origem BIGINT,
                ADD COLUMN IF NOT EXISTS numero_item INT,
                ADD COLUMN IF NOT EXISTS status_venda VARCHAR(20) NOT NULL DEFAULT ''CONCLUIDA'',
                ADD COLUMN IF NOT EXISTS excluido_origem BOOLEAN NOT NULL DEFAULT FALSE,
                ADD COLUMN IF NOT EXISTS execucao_id BIGINT REFERENCES etl.execucao(id)', modelo);
        EXECUTE format('
            CREATE UNIQUE INDEX IF NOT EXISTS uq_fato_item_origem
            ON %I.fato_venda (venda_id_origem, numero_item)', modelo);
        EXECUTE format('
            CREATE OR REPLACE VIEW %I.vendas_concluidas AS
            SELECT * FROM %I.fato_venda
            WHERE status_venda = ''CONCLUIDA'' AND NOT excluido_origem', modelo, modelo);
    END LOOP;
END
$migracao$;

-- Restrições de vigência também são aplicadas em bancos criados antes do ETL.
DO $restricoes$
DECLARE
    modelo TEXT;
BEGIN
    FOREACH modelo IN ARRAY ARRAY['dw_estrela', 'dw_floco_neve'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
            WHERE n.nspname = modelo AND c.conname = 'chk_cliente_vigencia'
        ) THEN
            EXECUTE format('
                ALTER TABLE %I.dim_cliente ADD CONSTRAINT chk_cliente_vigencia
                CHECK ((atual AND data_fim IS NULL)
                    OR (NOT atual AND data_fim IS NOT NULL AND data_fim > data_inicio))', modelo);
        END IF;
    END LOOP;
END
$restricoes$;
