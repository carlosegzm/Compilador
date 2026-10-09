package br.edu.ifsc.compiladores.lexico;

public enum TipoToken {

    // tipos básicos
    NUM_INT, NUM_DEC, ID, TEXTO,

    // palavras reservadas
    INT, FLOAT, CHAR, BOOLEAN, VOID, IF, ELSE, FOR, WHILE, SCANF, PRINTLN, MAIN, RETURN, FUNC,

    // Operadores de atribuição
    ATRIB, // =

    // operadores aritméticos
    SOMA, // +
    SUB, // -
    MULT, // *
    DIV, // /
    MOD, // %

    // operadores lógicos
    AND, // &&
    OR, // ||
    NOT, // !

    // operadores de comparação
    MAIOR, // >
    MAIOR_IGUAL, // >=
    MENOR, // <
    MENOR_IGUAL, // <=
    DIFERENTE, // !=
    IGUAL, // ==

    // símbolos especiais
    ABRE_PAR, // (
    FECHA_PAR, // )
    ABRE_COL, // [
    FECHA_COL, // ]
    ABRE_CHAVE, // {
    FECHA_CHAVE, // }
    VIRGULA, // ,
    PONTO_VIRGULA, // ;

    // fim de arquivo
    EOF

}
