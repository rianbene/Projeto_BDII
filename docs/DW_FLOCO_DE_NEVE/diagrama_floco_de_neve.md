::: mermaid
erDiagram
    %% Hierarquia Produto
    dim_departamento ||--o{ dim_categoria : "possui"
    dim_categoria ||--o{ dim_produto : "agrupa"
    
    %% Hierarquia Loja (Geográfica)
    dim_estado ||--o{ dim_cidade : "possui"
    dim_cidade ||--o{ dim_loja : "localiza"
    
    %% Fato e Dimensões Planas
    dim_tempo ||--o{ fato_venda : "registra"
    dim_produto ||--o{ fato_venda : "item"
    dim_cliente ||--o{ fato_venda : "comprador"
    dim_loja ||--o{ fato_venda : "origem"

    dim_departamento {
        int sk_departamento PK
        string nome
    }
    dim_categoria {
        int sk_categoria PK
        string nome
        int sk_departamento FK
    }
    dim_produto {
        int sk_produto PK
        bigint id_origem
        string nome
        string principio_ativo
        int sk_categoria FK
    }
    dim_estado {
        int sk_estado PK
        string nome
        char sigla
    }
    dim_cidade {
        int sk_cidade PK
        string nome
        int sk_estado FK
    }
    dim_loja {
        int sk_loja PK
        bigint id_origem
        string nome_fantasia
        string bairro
        int sk_cidade FK
    }
    dim_tempo {
        int sk_tempo PK
        date data
    }
    dim_cliente {
        int sk_cliente PK
        bigint id_origem
        string nome
    }
    fato_venda {
        bigint sk_fato PK
        int sk_tempo FK
        int sk_produto FK
        int sk_cliente FK
        int sk_loja FK
        int quantidade
        numeric valor_bruto
        numeric desconto
        numeric valor_liquido
    }
    