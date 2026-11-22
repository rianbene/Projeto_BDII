-- Criação do esquema (caso ainda não exista)
CREATE SCHEMA IF NOT EXISTS dw;

-- ==========================================
-- HIERARQUIA: DEPARTAMENTO -> CATEGORIA -> PRODUTO
-- ==========================================

CREATE TABLE dw.dim_departamento (
    sk_departamento SERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL
);

CREATE TABLE dw.dim_categoria (
    sk_categoria SERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL,
    sk_departamento INT NOT NULL REFERENCES dw.dim_departamento(sk_departamento)
);

CREATE TABLE dw.dim_produto (
    sk_produto SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    principio_ativo VARCHAR(100),
    sk_categoria INT NOT NULL REFERENCES dw.dim_categoria(sk_categoria)
);

-- ==========================================
-- HIERARQUIA: ESTADO -> CIDADE -> LOJA
-- ==========================================

CREATE TABLE dw.dim_estado (
    sk_estado SERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL,
    sigla CHAR(2) NOT NULL UNIQUE
);

CREATE TABLE dw.dim_cidade (
    sk_cidade SERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    sk_estado INT NOT NULL REFERENCES dw.dim_estado(sk_estado)
);

CREATE TABLE dw.dim_loja (
    sk_loja SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome_fantasia VARCHAR(100) NOT NULL,
    bairro VARCHAR(80),
    sk_cidade INT NOT NULL REFERENCES dw.dim_cidade(sk_cidade)
);

-- ==========================================
-- DIMENSÕES PLANAS (DESNORMALIZADAS)
-- ==========================================

CREATE TABLE dw.dim_tempo (
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

CREATE TABLE dw.dim_cliente (
    sk_cliente SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    data_nascimento DATE,
    faixa_etaria VARCHAR(20)
);

-- ==========================================
-- TABELA FATO
-- ==========================================

CREATE TABLE dw.fato_venda (
    sk_fato BIGSERIAL PRIMARY KEY,
    sk_tempo INT NOT NULL REFERENCES dw.dim_tempo(sk_tempo),
    sk_produto INT NOT NULL REFERENCES dw.dim_produto(sk_produto),
    sk_cliente INT NOT NULL REFERENCES dw.dim_cliente(sk_cliente),
    sk_loja INT NOT NULL REFERENCES dw.dim_loja(sk_loja),
    
    -- Medidas
    quantidade INT NOT NULL,
    valor_bruto NUMERIC(12,2) NOT NULL,
    desconto NUMERIC(12,2) DEFAULT 0.00,
    valor_liquido NUMERIC(12,2) NOT NULL
);

-- Índices para otimização de consultas analíticas
CREATE INDEX idx_fato_tempo ON dw.fato_venda(sk_tempo);
CREATE INDEX idx_fato_produto ON dw.fato_venda(sk_produto);
CREATE INDEX idx_fato_cliente ON dw.fato_venda(sk_cliente);
CREATE INDEX idx_fato_loja ON dw.fato_venda(sk_loja);