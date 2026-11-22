-- Criação do esquema separado para o Data Warehouse
CREATE SCHEMA IF NOT EXISTS dw;

-- Dimensão Tempo
CREATE TABLE dw.dim_tempo (
    sk_tempo INT PRIMARY KEY, -- Ex: YYYYMMDD
    data DATE NOT NULL,
    dia SMALLINT NOT NULL,
    mes SMALLINT NOT NULL,
    nome_mes VARCHAR(15) NOT NULL,
    trimestre SMALLINT NOT NULL,
    semestre SMALLINT NOT NULL,
    ano SMALLINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL
);

-- Dimensão Produto
CREATE TABLE dw.dim_produto (
    sk_produto SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL, -- Chave natural do OLTP
    nome VARCHAR(120) NOT NULL,
    principio_ativo VARCHAR(100),
    categoria VARCHAR(60),
    fabricante VARCHAR(80)
);

-- Dimensão Cliente
CREATE TABLE dw.dim_cliente (
    sk_cliente SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL, -- Chave natural do OLTP
    nome VARCHAR(120) NOT NULL,
    cidade VARCHAR(80),
    estado CHAR(2),
    data_nascimento DATE,
    faixa_etaria VARCHAR(20)
);

-- Dimensão Loja
CREATE TABLE dw.dim_loja (
    sk_loja SERIAL PRIMARY KEY,
    id_origem BIGINT NOT NULL, -- Chave natural do OLTP
    nome_fantasia VARCHAR(100) NOT NULL,
    bairro VARCHAR(80),
    cidade VARCHAR(80),
    estado CHAR(2)
);

-- Tabela Fato
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

-- Índices para otimizar consultas OLAP (Roll-up / Drill-down)
CREATE INDEX idx_fato_tempo ON dw.fato_venda(sk_tempo);
CREATE INDEX idx_fato_produto ON dw.fato_venda(sk_produto);
CREATE INDEX idx_fato_cliente ON dw.fato_venda(sk_cliente);
CREATE INDEX idx_fato_loja ON dw.fato_venda(sk_loja);