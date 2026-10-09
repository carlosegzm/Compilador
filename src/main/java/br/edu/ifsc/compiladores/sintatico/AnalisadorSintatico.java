package br.edu.ifsc.compiladores.sintatico;

import br.edu.ifsc.compiladores.lexico.Token;
import br.edu.ifsc.compiladores.lexico.TipoToken;

import java.util.List;

/**
 * AnalisadorSintatico
 * 
 * <h1> Implementação de um Analisador Sintático Descendente Recursivo. <h1>
 * 
 * <p> 
 *      Esta classe valida se a sequência de tokens está gramaticalmente correta. 
 *      Cada método representa uma regra de produção da gramática LL(1). 
 * <p>
 */
public class AnalisadorSintatico {
    private List<Token> tokens;
    private int posicao;

    public AnalisadorSintatico(List<Token> tokens) {
        this.tokens = tokens;
        this.posicao = 0;
    }

    // Retorna o ponteiro do token atual 
    // ou EOF se a posição for inválida
    private Token tokenAtual() {
        if (posicao < tokens.size()) {
            return tokens.get(posicao);
        }
        return tokens.get(tokens.size() - 1);
    }

    // Verifica se o token atual é o esperado. 
    // Se for, avança 
    // Se não, lança excessão
    private void consome(TipoToken esperado) {
        if (tokenAtual().getTipo() == esperado) {
            posicao++;
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() +
                    ": esperado " + esperado + ", mas encontrou " + tokenAtual().getTipo() + " ('" + tokenAtual().getLexema() + "')");
        }
    }

    // Inicia a análise sintática.
    public void analisar() {
        programa();

        // Depois de ler todo o programa, espera-se encontrar o fim do arquivo (EOF)
        if (tokenAtual().getTipo() != TipoToken.EOF) {
            throw new RuntimeException("Erro Sintático: Tokens adicionais encontrados após o fim do programa principal na linha " + tokenAtual().getLinha());
        }

        System.out.println("Analise Sintatica encerrada com exito");
    }

    // Regra: PROGRAMA → LISTAFUNÇÕES PRINCIPAL
    private void programa() {
        listaFuncoes();
        principal();
    }

    // Regra: LISTAFUNÇÕES → DECFUNÇÃO LISTAFUNÇÕES | ε
    // Enquanto o token atual não for a palavra "main", consideramos que são declarações de funções
    private void listaFuncoes() {
        if (tokenAtual().getTipo() != TipoToken.MAIN && tokenAtual().getTipo() != TipoToken.EOF) {
            decFuncao();
            listaFuncoes();
        }
    }

    // Regra: DECFUNÇÃO → TIPORETORNO id (PARAMETROS) BLOCO
    private void decFuncao() {
        tipoRetorno();
        consome(TipoToken.ID);
        consome(TipoToken.ABRE_PAR);
        parametros();
        consome(TipoToken.FECHA_PAR);
        bloco();
    }

    // Regra: TIPORETORNO → TIPO | void
    private void tipoRetorno() {
        if (tokenAtual().getTipo() == TipoToken.VOID) {
            consome(TipoToken.VOID);
        } else {
            tipo();
        }
    }

    // Regra: TIPO → TIPOBASE DIMENSÃO
    private void tipo() {
        tipoBase();
        dimensao();
    }

    // Regra: TIPOBASE → char | float | int | boolean
    private void tipoBase() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.CHAR || t == TipoToken.FLOAT || t == TipoToken.INT || t == TipoToken.BOOLEAN) {
            consome(t);
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado tipo base (char, float, int, boolean)");
        }
    }

    // Regra: DIMENSÃO → [num_int] DIMENSÃO | ε
    // Usada para identificar arrays na declaração
    private void dimensao() {
        if (tokenAtual().getTipo() == TipoToken.ABRE_COL) {
            consome(TipoToken.ABRE_COL);
            consome(TipoToken.NUM_INT);
            consome(TipoToken.FECHA_COL);
            dimensao();
        }
    }

    // Regra: PARAMETROS → TIPO id LISTAPARAMETROS | ε
    private void parametros() {
        TipoToken t = tokenAtual().getTipo();
        // Checamos se inicia com um tipo base
        if (t == TipoToken.CHAR || t == TipoToken.FLOAT || t == TipoToken.INT || t == TipoToken.BOOLEAN) {
            tipo();
            consome(TipoToken.ID);
            listaParametros();
        }
    }

    // Regra: LISTAPARAMETROS → , TIPO id LISTAPARAMETROS | ε
    private void listaParametros() {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            tipo();
            consome(TipoToken.ID);
            listaParametros();
        }
    }

    // Regra: PRINCIPAL → main () BLOCO
    private void principal() {
        consome(TipoToken.MAIN);
        consome(TipoToken.ABRE_PAR);
        consome(TipoToken.FECHA_PAR);
        bloco();
    }

    // Regra: BLOCO → { LISTAVARIÁVEIS COMANDOS }
    private void bloco() {
        consome(TipoToken.ABRE_CHAVE);
        listaVariaveis();
        comandos();
        consome(TipoToken.FECHA_CHAVE);
    }

    // Regra: LISTAVARIÁVEIS → TIPO id LISTAID; LISTAVARIÁVEIS | ε
    private void listaVariaveis() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.CHAR || t == TipoToken.FLOAT || t == TipoToken.INT || t == TipoToken.BOOLEAN) {
            tipo();
            consome(TipoToken.ID);
            listaId();
            consome(TipoToken.PONTO_VIRGULA);
            listaVariaveis();
        }
    }

    // Regra: LISTAID → , id LISTAID | ε
    // Permite declarar múltiplas variáveis do mesmo tipo na mesma linha (ex: int a, b, c;)
    private void listaId() {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            consome(TipoToken.ID);
            listaId();
        }
    }

    // Regra: COMANDOS → COMANDO COMANDOS | ε
    private void comandos() {
        TipoToken t = tokenAtual().getTipo();
        // Se for um dos tokens que iniciam comando, prosseguimos
        if (t == TipoToken.SCANF || t == TipoToken.PRINTLN || t == TipoToken.ID || 
            t == TipoToken.FUNC || t == TipoToken.IF || t == TipoToken.WHILE || t == TipoToken.RETURN) {
            comando();
            comandos();
        }
    }

    // Regra: COMANDO → LEITURA | ESCRITA | ATRIBUIÇÃO | FUNÇÃO | SELEÇÃO | ENQUANTO | RETORNO
    // Identifica e delega para a funcao do respectivo comando
    private void comando() {
        TipoToken t = tokenAtual().getTipo();
        switch (t) {
            case SCANF: leitura(); break;
            case PRINTLN: escrita(); break;
            case ID: atribuicao(); break;
            case FUNC: funcao(); break;
            case IF: selecao(); break;
            case WHILE: enquanto(); break;
            case RETURN: retorno(); break;
            default:
                throw new RuntimeException("Erro Sintático: Comando inesperado na linha " + tokenAtual().getLinha());
        }
    }

    // Regra: LEITURA → scanf (TERMOLEITURA NOVOTERMOLEITURA);
    private void leitura() {
        consome(TipoToken.SCANF);
        consome(TipoToken.ABRE_PAR);
        termoLeitura();
        novoTermoLeitura();
        consome(TipoToken.FECHA_PAR);
        consome(TipoToken.PONTO_VIRGULA);
    }

    // Regra: TERMOLEITURA → id DIMENSAO2
    private void termoLeitura() {
        consome(TipoToken.ID);
        dimensao2();
    }

    // Regra: NOVOTERMOLEITURA → , TERMOLEITURA NOVOTERMOLEITURA | ε
    private void novoTermoLeitura() {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            termoLeitura();
            novoTermoLeitura();
        }
    }

    // Regra: DIMENSAO2 → [EXPR_ADITIVA ] DIMENSAO2 | ε
    // Usada para acesso a elementos de array
    private void dimensao2() {
        if (tokenAtual().getTipo() == TipoToken.ABRE_COL) {
            consome(TipoToken.ABRE_COL);
            exprAditiva();
            consome(TipoToken.FECHA_COL);
            dimensao2();
        }
    }

    // Regra: ESCRITA → println (TERMOESCRITA NOVOTERMOESCRITA);
    private void escrita() {
        consome(TipoToken.PRINTLN);
        consome(TipoToken.ABRE_PAR);
        termoEscrita();
        novoTermoEscrita();
        consome(TipoToken.FECHA_PAR);
        consome(TipoToken.PONTO_VIRGULA);
    }

    // Regra: TERMOESCRITA → id DIMENSAO2 | CONSTANTE | texto
    private void termoEscrita() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.ID) {
            consome(TipoToken.ID);
            dimensao2();
        } else if (t == TipoToken.NUM_INT || t == TipoToken.NUM_DEC) {
            constante();
        } else if (t == TipoToken.TEXTO) {
            consome(TipoToken.TEXTO);
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado variável, constante ou texto no println");
        }
    }

    // Regra: NOVOTERMOESCRITA → , TERMOESCRITA NOVOTERMOESCRITA | ε
    private void novoTermoEscrita() {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            termoEscrita();
            novoTermoEscrita();
        }
    }

    // Regra: SELEÇÃO → if (EXPRESSÃO) BLOCO SENÃO
    private void selecao() {
        consome(TipoToken.IF);
        consome(TipoToken.ABRE_PAR);
        expressao();
        consome(TipoToken.FECHA_PAR);
        bloco();
        senao();
    }

    // Regra: SENÃO → else BLOCO | ε
    private void senao() {
        if (tokenAtual().getTipo() == TipoToken.ELSE) {
            consome(TipoToken.ELSE);
            bloco();
        }
    }

    // Regra: ENQUANTO → while (EXPRESSÃO) BLOCO
    private void enquanto() {
        consome(TipoToken.WHILE);
        consome(TipoToken.ABRE_PAR);
        expressao();
        consome(TipoToken.FECHA_PAR);
        bloco();
    }

    // Regra: ATRIBUIÇÃO → id = COMPLEMENTO;
    private void atribuicao() {
        consome(TipoToken.ID);
        consome(TipoToken.ATRIB);
        complemento();
        consome(TipoToken.PONTO_VIRGULA);
    }

    // Regra: COMPLEMENTO → EXPRESSÃO | FUNÇÃO
    private void complemento() {
        if (tokenAtual().getTipo() == TipoToken.FUNC) {
            funcao();
        } else {
            expressao();
        }
    }

    // Regra: FUNÇÃO → func id (ARGUMENTOS)
    // Chamada de função.
    private void funcao() {
        consome(TipoToken.FUNC);
        consome(TipoToken.ID);
        consome(TipoToken.ABRE_PAR);
        argumentos();
        consome(TipoToken.FECHA_PAR);
    }

    // Regra: ARGUMENTOS → EXPRESSÃO NOVO_ARGUMENTO | ε
    private void argumentos() {
        if (tokenAtual().getTipo() != TipoToken.FECHA_PAR) {
            expressao();
            novoArgumento();
        }
    }

    // Regra: NOVO_ARGUMENTO → , EXPRESSÃO NOVO_ARGUMENTO | ε
    private void novoArgumento() {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            expressao();
            novoArgumento();
        }
    }

    // Regra: RETORNO → return EXPRESSÃO;
    private void retorno() {
        consome(TipoToken.RETURN);
        expressao();
        consome(TipoToken.PONTO_VIRGULA);
    }

    // Regra: EXPRESSÃO → EXPR_OU
    private void expressao() {
        exprOu();
    }

    // Regra: EXPR_OU → EXPR_E EXPR_OU2
    private void exprOu() {
        exprE();
        exprOu2();
    }

    // Regra: EXPR_OU2 → || EXPR_E EXPR_OU2 | ε
    private void exprOu2() {
        if (tokenAtual().getTipo() == TipoToken.OR) {
            consome(TipoToken.OR);
            exprE();
            exprOu2();
        }
    }

    // Regra: EXPR_E → EXPR_RELACIONAL EXPR_E2
    private void exprE() {
        exprRelacional();
        exprE2();
    }

    // Regra: EXPR_E2 → && EXPR_RELACIONAL EXPR_E2 | ε
    private void exprE2() {
        if (tokenAtual().getTipo() == TipoToken.AND) {
            consome(TipoToken.AND);
            exprRelacional();
            exprE2();
        }
    }

    // Regra: EXPR_RELACIONAL → EXPR_ADITIVA EXPR_RELACIONAL2
    private void exprRelacional() {
        exprAditiva();
        exprRelacional2();
    }

    // Regra: EXPR_RELACIONAL2 → comp EXPR_ADITIVA | ε
    private void exprRelacional2() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.MAIOR || t == TipoToken.MAIOR_IGUAL || 
            t == TipoToken.MENOR || t == TipoToken.MENOR_IGUAL || 
            t == TipoToken.DIFERENTE || t == TipoToken.IGUAL) {
            consome(t);
            exprAditiva();
        }
    }

    // Regra: EXPR_ADITIVA → EXPR_MULTIPLICATIVA EXPR_ADITIVA2
    private void exprAditiva() {
        exprMultiplicativa();
        exprAditiva2();
    }

    // Regra: EXPR_ADITIVA2 → OP_ADITIVO EXPR_MULTIPLICATIVA EXPR_ADITIVA2 | ε
    private void exprAditiva2() {
        if (tokenAtual().getTipo() == TipoToken.SOMA || tokenAtual().getTipo() == TipoToken.SUB) {
            opAditivo();
            exprMultiplicativa();
            exprAditiva2();
        }
    }

    // Regra: OP_ADITIVO → + | -
    private void opAditivo() {
        if (tokenAtual().getTipo() == TipoToken.SOMA) {
            consome(TipoToken.SOMA);
        } else if (tokenAtual().getTipo() == TipoToken.SUB) {
            consome(TipoToken.SUB);
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado + ou -");
        }
    }

    // Regra: EXPR_MULTIPLICATIVA → FATOR EXPR_MULTIPLICATIVA2
    private void exprMultiplicativa() {
        fator();
        exprMultiplicativa2();
    }

    // Regra: EXPR_MULTIPLICATIVA2 → OP_MULTIPLICATIVO FATOR EXPR_MULTIPLICATIVA2 | ε
    private void exprMultiplicativa2() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.MULT || t == TipoToken.DIV || t == TipoToken.MOD) {
            opMultiplicativo();
            fator();
            exprMultiplicativa2();
        }
    }

    // Regra: OP_MULTIPLICATIVO → * | / | %
    private void opMultiplicativo() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.MULT) {
            consome(TipoToken.MULT);
        } else if (t == TipoToken.DIV) {
            consome(TipoToken.DIV);
        } else if (t == TipoToken.MOD) {
            consome(TipoToken.MOD);
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado *, / ou %");
        }
    }

    // Regra: FATOR → SINAL TERMO | texto | ! FATOR | (EXPRESSÃO)
    private void fator() {
        TipoToken t = tokenAtual().getTipo();
        
        if (t == TipoToken.SOMA || t == TipoToken.SUB || t == TipoToken.ID || t == TipoToken.NUM_INT || t == TipoToken.NUM_DEC) {
            sinal(); // pode ser ε
            termo();
        } else if (t == TipoToken.TEXTO) {
            consome(TipoToken.TEXTO);
        } else if (t == TipoToken.NOT) {
            consome(TipoToken.NOT);
            fator();
        } else if (t == TipoToken.ABRE_PAR) {
            consome(TipoToken.ABRE_PAR);
            expressao();
            consome(TipoToken.FECHA_PAR);
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Fator inválido. Token inesperado: " + t);
        }
    }

    // Regra: TERMO → id DIMENSAO2 | CONSTANTE
    private void termo() {
        if (tokenAtual().getTipo() == TipoToken.ID) {
            consome(TipoToken.ID);
            dimensao2();
        } else {
            constante();
        }
    }

    // Regra: SINAL → + | - | ε
    private void sinal() {
        if (tokenAtual().getTipo() == TipoToken.SOMA) {
            consome(TipoToken.SOMA);
        } else if (tokenAtual().getTipo() == TipoToken.SUB) {
            consome(TipoToken.SUB);
        }
    }

    // Regra: CONSTANTE → num_int | num_dec
    private void constante() {
        if (tokenAtual().getTipo() == TipoToken.NUM_INT) {
            consome(TipoToken.NUM_INT);
        } else if (tokenAtual().getTipo() == TipoToken.NUM_DEC) {
            consome(TipoToken.NUM_DEC);
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado constante numérica");
        }
    }
}
