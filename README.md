# Compilador C-Simplificado

Este projeto é uma implementação educacional das fases de **Análise Léxica (Scanner)**, **Análise Sintática (Parser)**, **Análise Semântica** e **Geração de Código Intermediário** (código de três endereços) para a linguagem hipotética "C-Simplificado". 
Desenvolvido em Java, sem o uso de geradores de parser (como Lex, YACC ou AntLR). Foi planejado para ser apresentado em trabalhos universitários, adotando extrema simplicidade e didática.

## Arquitetura do Projeto

O código está estruturado para ser extremamente didático, não possui padrões de projeto complexos, utilizando uma abordagem imperativa simples (Recursive Descent Parser). 

- `src/lexico/`: Contém o Analisador Léxico, que lê os caracteres e os agrupa em tokens identificáveis.
- `src/sintatico/`: Contém o Analisador Sintático Descendente Recursivo, com exatamente um método Java mapeado para cada regra da gramática LL(1) original. As ações semânticas e a geração de código são feitas dentro desses mesmos métodos (tradução dirigida pela sintaxe, em uma única passada).
- `src/semantico/`: Analisador Semântico, com a tabela de símbolos (variáveis por função e funções com seus parâmetros) e as regras de tipo.
- `src/intermediario/`: Gerador do código de três endereços (`newTemp`, `nextinstr`, `emit`, `makelist`, `merge`, `backpatch`) e os atributos das expressões (`tipo`, `addr`, `truelist`, `falselist`).
- `src/Main`: Entrypoint da aplicação que trata os argumentos de entrada e orquestra as fases da compilação.

``` 
Compilador
├── Makefile
├── pom.xml
├── src
│   └── main
│       └── java
│           └── br
│               └── edu
│                   └── ifsc
│                       └── compiladores
│                           ├── intermediario
│                           │   ├── Expressao.java
│                           │   └── GeradorCodigo.java
│                           ├── lexico
│                           │   ├── AnalisadorLexico.java
│                           │   ├── TipoToken.java
│                           │   └── Token.java
│                           ├── Main.java
│                           ├── semantico
│                           │   └── AnalisadorSemantico.java
│                           └── sintatico
│                               └── AnalisadorSintatico.java
├── test_batch
│   ├── lex_caractere_inesperado.txt
│   ├── lex_decimal_incompleto.txt
│   ├── lex_numero_com_letra.txt
│   ├── lex_ponto_extra.txt
│   ├── lex_texto_nao_fechado.txt
│   ├── ok_if_else.txt
│   ├── ok_teste.txt
│   ├── ok_while_curto_circuito.txt
│   ├── README.md
│   ├── sem_atribuicao_tipo.txt
│   ├── sem_condicao_nao_boolean.txt
│   ├── sem_declaracao_duplicada.txt
│   ├── sem_quantidade_argumentos.txt
│   ├── sem_retorno_tipo.txt
│   ├── sem_variavel_nao_declarada.txt
│   ├── sint_dec_ordem_errada.txt
│   └── sint_dec_sem_virgula.txt
└── teste.txt
```


## Como rodar

### Compilar

```bash
mvn clean package
```

Ou usando Make

```bash
make build
```

Isso gera o jar `target/Compilador-1.0-SNAPSHOT.jar`.

### Analisar arquivos

```bash
java -cp target/Compilador-1.0-SNAPSHOT.jar br.edu.ifsc.compiladores.Main [-v] <arquivo|pasta> [<arquivo|pasta> ...]
```

Ou usando Make

```bash
make run FLAGS=[-v] INPUT_PATH=<arquivo|pasta> [<arquivo|pasta> ...]
```

- Aceita **um ou vários** arquivos e/ou pastas (de pastas, só são lidos os `.txt`).
- `-v` exibe os tokens gerados pelo analisador léxico e, se não houver erro, a tabela de símbolos e o código de três endereços.
- Para cada arquivo, o programa informa se a análise terminou em `OK`,
  `ERRO_LEXICO`, `ERRO_SINTATICO` ou `ERRO_SEMANTICO`, junto com a mensagem de erro.

Exemplos:

```bash
# um único arquivo, mostrando os tokens, a tabela de símbolos e o código gerado
java -cp target/Compilador-1.0-SNAPSHOT.jar br.edu.ifsc.compiladores.Main -v teste.txt

# bateria completa de testes
java -cp target/Compilador-1.0-SNAPSHOT.jar br.edu.ifsc.compiladores.Main test_batch/
```

> Mais exemplos de como usar as flags e teste de múltiplos arquivos estão presentes em [`Makefile`](Makefile).

### Rodar os testes

```bash
make test
```

Compila o projeto e executa todos os arquivos de `test_batch/`. O resultado esperado de
cada teste é definido pelo prefixo do nome (`ok_`, `lex_`, `sint_`, `sem_`). O programa
retorna código de saída diferente de zero se algum teste falhar.

> Detalhes sobre a convenção de nomes e a leitura da saída estão em [`test_batch/README.md`](test_batch/README.md).

Ao executar com `-v`, o programa imprimirá todos os tokens gerados pelo analisador léxico, a tabela de símbolos, o código de três endereços e, em seguida, o resultado da análise.