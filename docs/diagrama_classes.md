:::mermaid
classDiagram
    class Pessoa {
        <<abstract>>
        +Long id
        +String nome
        +String cpf
        +Date dataNascimento
        +String telefone
        +String email
    }

    class Cliente {
        +String numeroCartaoFidelidade
        +Date dataCadastro
        +boolean ativo
    }

    class Funcionario {
        +String matricula
        +Date dataAdmissao
        +BigDecimal salario
        +boolean ativo
    }

    class Endereco {
        +String rua
        +String numero
        +String complemento
        +String bairro
        +String cidade
        +String estado
        +String cep
    }

    class UnidadeFarmacia {
        +String nome
        +String cnpj
        +String telefone
        +boolean ativa
    }

    class Departamento {
        +String nome
        +String descricao
    }

    class Categoria {
        +String nome
        +String descricao
    }

    class Medicamento {
        +String nome
        +String principioAtivo
        +String codigoBarras
        +String laboratorio
        +String apresentacao
        +BigDecimal precoBase
        +Integer estoque
    }

    class Venda {
        +Date dataHora
        +BigDecimal valorTotal
        +String observacao
        +calcularValorTotal()
    }

    class ItemVenda {
        +Integer numeroItem
        +Integer quantidade
        +BigDecimal precoUnitario
        +BigDecimal desconto
        +BigDecimal valorLiquido
        +calcularValorLiquido()
    }

    class Pagamento {
        +BigDecimal valor
        +Date dataHora
        +String numeroTransacao
        +boolean aprovado
        +confirmar()
        +cancelar()
    }

    class CargoFuncionario {
        <<enumeration>>
        FARMACEUTICO
        ATENDENTE
        GERENTE
    }

    class FormaPagamento {
        <<enumeration>>
        DINHEIRO
        CARTAO_CREDITO
        CARTAO_DEBITO
        PIX
    }

    class StatusVenda {
        <<enumeration>>
        CONCLUIDA
        CANCELADA
    }

    class TipoControleMedicamento {
        <<enumeration>>
        ISENTO
        TARJA_VERMELHA
        CONTROLADO
    }

    Pessoa <|-- Cliente
    Pessoa <|-- Funcionario

    Pessoa "1" *-- "1" Endereco : possui
    UnidadeFarmacia "1" *-- "1" Endereco : possui

    Funcionario "0..*" --> "1" UnidadeFarmacia : trabalhaEm
    Funcionario "1" --> "1" CargoFuncionario : ocupa

    Departamento "1" *-- "0..*" Categoria : agrupa
    Categoria "1" -- "0..*" Medicamento : classifica

    Medicamento "1" --> "1" TipoControleMedicamento : possuiTipo

    Cliente "1" -- "0..*" Venda : realiza
    UnidadeFarmacia "1" -- "0..*" Venda : registra
    Funcionario "1" -- "0..*" Venda : atende

    Venda "1" *-- "1..*" ItemVenda : contem
    ItemVenda "0..*" --> "1" Medicamento : referencia

    Venda "1" *-- "1" Pagamento : possui
    Pagamento "1" --> "1" FormaPagamento : utiliza
    Venda "1" --> "1" StatusVenda : possuiStatus
:::