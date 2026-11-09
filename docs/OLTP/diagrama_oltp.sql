CREATE TABLE unidade_farmacia (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    rua VARCHAR(120) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(120),
    bairro VARCHAR(80) NOT NULL,
    cidade VARCHAR(80) NOT NULL,
    estado CHAR(2) NOT NULL,
    cep VARCHAR(8) NOT NULL
);

CREATE TABLE pessoa (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    rua VARCHAR(120) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(120),
    bairro VARCHAR(80) NOT NULL,
    cidade VARCHAR(80) NOT NULL,
    estado CHAR(2) NOT NULL,
    cep VARCHAR(8) NOT NULL
);

CREATE TABLE cliente (
    id BIGINT PRIMARY KEY REFERENCES pessoa(id) ON DELETE CASCADE,
    numero_cartao_fidelidade VARCHAR(30) NOT NULL UNIQUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE funcionario (
    id BIGINT PRIMARY KEY REFERENCES pessoa(id) ON DELETE CASCADE,
    matricula VARCHAR(30) NOT NULL UNIQUE,
    data_admissao DATE NOT NULL,
    salario NUMERIC(12,2) NOT NULL CHECK (salario >= 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    cargo VARCHAR(20) NOT NULL CHECK (cargo IN ('FARMACEUTICO', 'ATENDENTE', 'GERENTE')),
    unidade_id BIGINT NOT NULL REFERENCES unidade_farmacia(id)
);

CREATE TABLE departamento (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);

CREATE TABLE categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    descricao VARCHAR(255),
    departamento_id BIGINT NOT NULL REFERENCES departamento(id),
    UNIQUE (departamento_id, nome)
);

CREATE TABLE medicamento (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    principio_ativo VARCHAR(120) NOT NULL,
    codigo_barras VARCHAR(30) NOT NULL UNIQUE,
    laboratorio VARCHAR(120) NOT NULL,
    apresentacao VARCHAR(80) NOT NULL,
    preco_base NUMERIC(12,2) NOT NULL CHECK (preco_base >= 0),
    estoque INTEGER NOT NULL DEFAULT 0 CHECK (estoque >= 0),
    categoria_id BIGINT NOT NULL REFERENCES categoria(id),
    tipo_controle VARCHAR(20) NOT NULL CHECK (tipo_controle IN ('ISENTO', 'TARJA_VERMELHA', 'CONTROLADO'))
);

CREATE TABLE venda (
    id BIGSERIAL PRIMARY KEY,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valor_total NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (valor_total >= 0),
    observacao TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CONCLUIDA' CHECK (status IN ('CONCLUIDA', 'CANCELADA')),
    cliente_id BIGINT NOT NULL REFERENCES cliente(id),
    unidade_id BIGINT NOT NULL REFERENCES unidade_farmacia(id),
    funcionario_id BIGINT NOT NULL REFERENCES funcionario(id)
);

CREATE TABLE item_venda (
    venda_id BIGINT NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    numero_item INTEGER NOT NULL CHECK (numero_item > 0),
    medicamento_id BIGINT NOT NULL REFERENCES medicamento(id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(12,2) NOT NULL CHECK (preco_unitario >= 0),
    desconto NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (desconto >= 0),
    valor_liquido NUMERIC(12,2) NOT NULL CHECK (valor_liquido >= 0),
    PRIMARY KEY (venda_id, numero_item)
);

CREATE TABLE pagamento (
    venda_id BIGINT PRIMARY KEY REFERENCES venda(id) ON DELETE CASCADE,
    valor NUMERIC(12,2) NOT NULL CHECK (valor >= 0),
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    numero_transacao VARCHAR(60) UNIQUE,
    aprovado BOOLEAN NOT NULL DEFAULT FALSE,
    forma_pagamento VARCHAR(20) NOT NULL CHECK (forma_pagamento IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX'))
);