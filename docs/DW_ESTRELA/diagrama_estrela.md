::: mermaid
erDiagram
    dim_tempo ||--o{ fato_venda : "registra"
    dim_produto ||--o{ fato_venda : "item"
    dim_cliente ||--o{ fato_venda : "comprador"
    dim_loja ||--o{ fato_venda : "origem"

    dim_tempo {
        int sk_tempo PK
        date data
        int dia
        int mes
        string nome_mes
        int trimestre
        int semestre
        int ano
        string dia_semana
    }

    dim_produto {
        int sk_produto PK
        bigint id_origem
        string nome
        string principio_ativo
        string categoria
        string fabricante
    }

    dim_cliente {
        int sk_cliente PK
        bigint id_origem
        string nome
        string cidade
        string estado
        date data_nascimento
        string faixa_etaria
    }

    dim_loja {
        int sk_loja PK
        bigint id_origem
        string nome_fantasia
        string bairro
        string cidade
        string estado
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