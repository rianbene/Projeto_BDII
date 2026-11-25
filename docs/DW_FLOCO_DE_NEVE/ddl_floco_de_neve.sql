DROP SCHEMA IF EXISTS dw_floco_neve CASCADE;
CREATE SCHEMA dw_floco_neve;

CREATE TABLE dw_floco_neve.dim_tempo (
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

CREATE TABLE dw_floco_neve.dim_departamento (
    sk_departamento SERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE dw_floco_neve.dim_categoria (
    sk_categoria SERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL,
    sk_departamento INT NOT NULL REFERENCES dw_floco_neve.dim_departamento(sk_departamento),
    CONSTRAINT uq_categoria UNIQUE (nome, sk_departamento)
);

CREATE TABLE dw_floco_neve.dim_produto (
    sk_produto SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    principio_ativo VARCHAR(100),
    sk_categoria INT NOT NULL REFERENCES dw_floco_neve.dim_categoria(sk_categoria)
);

CREATE TABLE dw_floco_neve.dim_estado (
    sk_estado SERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL,
    sigla CHAR(2) NOT NULL UNIQUE
);

CREATE TABLE dw_floco_neve.dim_cidade (
    sk_cidade SERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    sk_estado INT NOT NULL REFERENCES dw_floco_neve.dim_estado(sk_estado),
    CONSTRAINT uq_cidade UNIQUE (nome, sk_estado)
);

CREATE TABLE dw_floco_neve.dim_loja (
    sk_loja SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome_fantasia VARCHAR(100) NOT NULL,
    bairro VARCHAR(80),
    sk_cidade INT NOT NULL REFERENCES dw_floco_neve.dim_cidade(sk_cidade)
);

CREATE TABLE dw_floco_neve.dim_cliente (
    sk_cliente SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    cidade VARCHAR(80),
    estado CHAR(2),
    data_nascimento DATE,
    faixa_etaria VARCHAR(20)
);

CREATE TABLE dw_floco_neve.fato_venda (
    sk_fato BIGSERIAL PRIMARY KEY,
    sk_tempo INT NOT NULL REFERENCES dw_floco_neve.dim_tempo(sk_tempo),
    sk_produto INT NOT NULL REFERENCES dw_floco_neve.dim_produto(sk_produto),
    sk_cliente INT NOT NULL REFERENCES dw_floco_neve.dim_cliente(sk_cliente),
    sk_loja INT NOT NULL REFERENCES dw_floco_neve.dim_loja(sk_loja),

    quantidade INT NOT NULL,
    valor_bruto NUMERIC(12,2) NOT NULL,
    desconto NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_liquido NUMERIC(12,2) NOT NULL,

    CONSTRAINT chk_floco_venda_medidas
    CHECK (quantidade > 0 AND valor_bruto >= 0 AND desconto >= 0 AND valor_liquido >= 0)
);

CREATE INDEX idx_fato_floco_tempo ON dw_floco_neve.fato_venda(sk_tempo);
CREATE INDEX idx_fato_floco_produto ON dw_floco_neve.fato_venda(sk_produto);
CREATE INDEX idx_fato_floco_cliente ON dw_floco_neve.fato_venda(sk_cliente);
CREATE INDEX idx_fato_floco_loja ON dw_floco_neve.fato_venda(sk_loja);