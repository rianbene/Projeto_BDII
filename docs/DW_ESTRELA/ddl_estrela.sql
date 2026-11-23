CREATE SCHEMA IF NOT EXISTS dw_estrela;

CREATE TABLE IF NOT EXISTS dw_estrela.dim_tempo (
    sk_tempo INT PRIMARY KEY,
    data DATE NOT NULL,
    dia SMALLINT NOT NULL,
    mes SMALLINT NOT NULL,
    nome_mes VARCHAR(15) NOT NULL,
    trimestre SMALLINT NOT NULL,
    semestre SMALLINT NOT NULL,
    ano SMALLINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS dw_estrela.dim_produto (
    sk_produto SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    principio_ativo VARCHAR(100),
    categoria VARCHAR(60),
    fabricante VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS dw_estrela.dim_cliente (
    sk_cliente SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    cidade VARCHAR(80),
    estado CHAR(2),
    data_nascimento DATE,
    faixa_etaria VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS dw_estrela.dim_loja (
    sk_loja SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome_fantasia VARCHAR(100) NOT NULL,
    bairro VARCHAR(80),
    cidade VARCHAR(80),
    estado CHAR(2)
);

CREATE TABLE IF NOT EXISTS dw_estrela.fato_venda (
    sk_fato BIGSERIAL PRIMARY KEY,
    sk_tempo INT NOT NULL REFERENCES dw_estrela.dim_tempo(sk_tempo),
    sk_produto INT NOT NULL REFERENCES dw_estrela.dim_produto(sk_produto),
    sk_cliente INT NOT NULL REFERENCES dw_estrela.dim_cliente(sk_cliente),
    sk_loja INT NOT NULL REFERENCES dw_estrela.dim_loja(sk_loja),

    quantidade INT NOT NULL,
    valor_bruto NUMERIC(12,2) NOT NULL,
    desconto NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_liquido NUMERIC(12,2) NOT NULL,

    CONSTRAINT chk_fato_venda_medidas
    CHECK (quantidade > 0 AND valor_bruto >= 0 AND desconto >= 0 AND valor_liquido >= 0)
);

CREATE INDEX IF NOT EXISTS idx_fato_estrela_tempo ON dw_estrela.fato_venda(sk_tempo);
CREATE INDEX IF NOT EXISTS idx_fato_estrela_produto ON dw_estrela.fato_venda(sk_produto);
CREATE INDEX IF NOT EXISTS idx_fato_estrela_cliente ON dw_estrela.fato_venda(sk_cliente);
CREATE INDEX IF NOT EXISTS idx_fato_estrela_loja ON dw_estrela.fato_venda(sk_loja);
