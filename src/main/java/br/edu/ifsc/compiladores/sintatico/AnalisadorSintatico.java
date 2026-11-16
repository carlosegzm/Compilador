package br.edu.ifsc.compiladores.sintatico;

import br.edu.ifsc.compiladores.intermediario.Expressao;
import br.edu.ifsc.compiladores.intermediario.GeradorCodigo;
import br.edu.ifsc.compiladores.lexico.Token;
import br.edu.ifsc.compiladores.lexico.TipoToken;
import br.edu.ifsc.compiladores.semantico.AnalisadorSemantico;

import java.util.ArrayList;
import java.util.List;

import static br.edu.ifsc.compiladores.intermediario.GeradorCodigo.makelist;

/**
 * AnalisadorSintatico
 *
 * <h1> Implementação de um Analisador Sintático Descendente Recursivo. <h1>
 *
 * <p>
 *      Esta classe valida se a sequência de tokens está gramaticalmente correta.
 *      Cada método representa uma regra de produção da gramática LL(1).
 * <p>
 * <p>
 *      Junto com cada regra, o semântico confere os tipos e o gerador emite o código
 *      de três endereços (tradução dirigida pela sintaxe, em uma única passada).
 * <p>
 */
public class AnalisadorSintatico {
    private List<Token> tokens;
    private int posicao;

    private final AnalisadorSemantico semantico = new AnalisadorSemantico();
    private final GeradorCodigo codigo = new GeradorCodigo();

    public AnalisadorSintatico(List<Token> tokens) {
        this.tokens = tokens;
        this.posicao = 0;
    }

    public AnalisadorSemantico getSemantico() { return semantico; }
    public GeradorCodigo getCodigo() { return codigo; }

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
    // A função entra na tabela antes do corpo, para permitir chamada recursiva
    private void decFuncao() {
        String retorno = tipoRetorno();
        Token nome = tokenAtual();
        consome(TipoToken.ID);
        consome(TipoToken.ABRE_PAR);
        semantico.iniciarFuncao(nome.getLexema());
        List<String> parametros = parametros();
        consome(TipoToken.FECHA_PAR);
        semantico.declararFuncao(nome, retorno, parametros);

        codigo.emit("inicio_funcao " + nome.getLexema());
        bloco();
        codigo.emit("fim_funcao " + nome.getLexema());
    }

    // Regra: TIPORETORNO → TIPO | void
    private String tipoRetorno() {
        if (tokenAtual().getTipo() == TipoToken.VOID) {
            consome(TipoToken.VOID);
            return "void";
        }
        return tipo();
    }

    // Regra: TIPO → TIPOBASE DIMENSÃO
    // Ex: int [2][3] vira "int[2][3]"
    private String tipo() {
        return tipoBase() + dimensao();
    }

    // Regra: TIPOBASE → char | float | int | boolean
    private String tipoBase() {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.CHAR || t == TipoToken.FLOAT || t == TipoToken.INT || t == TipoToken.BOOLEAN) {
            String nome = tokenAtual().getLexema();
            consome(t);
            return nome;
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado tipo base (char, float, int, boolean)");
        }
    }

    // Regra: DIMENSÃO → [num_int] DIMENSÃO | ε
    // Usada para identificar arrays na declaração
    private String dimensao() {
        if (tokenAtual().getTipo() == TipoToken.ABRE_COL) {
            consome(TipoToken.ABRE_COL);
            Token tamanho = tokenAtual();
            consome(TipoToken.NUM_INT);
            semantico.verificarTamanho(tamanho);
            consome(TipoToken.FECHA_COL);
            return "[" + tamanho.getLexema() + "]" + dimensao();
        }
        return "";
    }

    // Regra: PARAMETROS → TIPO id LISTAPARAMETROS | ε
    // Cada parâmetro é declarado como variável da função. Retorna os tipos dos parâmetros
    private List<String> parametros() {
        List<String> tipos = new ArrayList<>();
        TipoToken t = tokenAtual().getTipo();
        // Checamos se inicia com um tipo base
        if (t == TipoToken.CHAR || t == TipoToken.FLOAT || t == TipoToken.INT || t == TipoToken.BOOLEAN) {
            String tipo = tipo();
            semantico.declararVariavel(tokenAtual(), tipo);
            consome(TipoToken.ID);
            tipos.add(tipo);
            listaParametros(tipos);
        }
        return tipos;
    }

    // Regra: LISTAPARAMETROS → , TIPO id LISTAPARAMETROS | ε
    private void listaParametros(List<String> tipos) {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            String tipo = tipo();
            semantico.declararVariavel(tokenAtual(), tipo);
            consome(TipoToken.ID);
            tipos.add(tipo);
            listaParametros(tipos);
        }
    }

    // Regra: PRINCIPAL → main () BLOCO
    private void principal() {
        consome(TipoToken.MAIN);
        consome(TipoToken.ABRE_PAR);
        consome(TipoToken.FECHA_PAR);
        semantico.iniciarFuncao("main");
        codigo.emit("inicio_main");
        bloco();
        codigo.emit("fim");
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
            String tipo = tipo();
            semantico.declararVariavel(tokenAtual(), tipo);
            consome(TipoToken.ID);
            listaId(tipo);
            consome(TipoToken.PONTO_VIRGULA);
            listaVariaveis();
        }
    }

    // Regra: LISTAID → , id LISTAID | ε
    // Permite declarar múltiplas variáveis do mesmo tipo na mesma linha (ex: int a, b, c;)
    private void listaId(String tipo) {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            semantico.declararVariavel(tokenAtual(), tipo);
            consome(TipoToken.ID);
            listaId(tipo);
        }
    }

    // Regra: COMANDOS → COMANDO COMANDOS | ε
    // Equivale a L → L1 M S: os gotos pendentes de um comando vão para o início do próximo
    private void comandos() {
        TipoToken t = tokenAtual().getTipo();
        // Se for um dos tokens que iniciam comando, prosseguimos
        if (t == TipoToken.SCANF || t == TipoToken.PRINTLN || t == TipoToken.ID ||
            t == TipoToken.FUNC || t == TipoToken.IF || t == TipoToken.WHILE || t == TipoToken.RETURN) {
            List<Integer> nextlist = comando();
            codigo.backpatch(nextlist, codigo.nextinstr());
            comandos();
        }
    }

    // Regra: COMANDO → LEITURA | ESCRITA | ATRIBUIÇÃO | FUNÇÃO | SELEÇÃO | ENQUANTO | RETORNO
    // Identifica e delega para a funcao do respectivo comando. Retorna a nextlist do comando
    private List<Integer> comando() {
        TipoToken t = tokenAtual().getTipo();
        switch (t) {
            case SCANF: leitura(); break;
            case PRINTLN: escrita(); break;
            case ID: atribuicao(); break;
            case FUNC: funcao(); break;
            case IF: return selecao();
            case WHILE: return enquanto();
            case RETURN: retorno(); break;
            default:
                throw new RuntimeException("Erro Sintático: Comando inesperado na linha " + tokenAtual().getLinha());
        }
        return new ArrayList<>(); // os outros comandos não deixam goto pendente
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
        Token id = tokenAtual();
        consome(TipoToken.ID);
        Expressao variavel = variavel(id);
        semantico.verificarNaoVetor(id, variavel.tipo());
        codigo.emit("read " + variavel.addr());
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
    // Usada para acesso a elementos de array. Retorna os índices, ex: ["[i]", "[t1]"]
    private List<String> dimensao2() {
        List<String> indices = new ArrayList<>();
        if (tokenAtual().getTipo() == TipoToken.ABRE_COL) {
            int linha = tokenAtual().getLinha();
            consome(TipoToken.ABRE_COL);
            Expressao indice = exprAditiva();
            semantico.verificarIndice(linha, indice.tipo());
            consome(TipoToken.FECHA_COL);
            indices.add("[" + indice.addr() + "]");
            indices.addAll(dimensao2());
        }
        return indices;
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
            Token id = tokenAtual();
            consome(TipoToken.ID);
            Expressao variavel = variavel(id);
            semantico.verificarNaoVetor(id, variavel.tipo());
            codigo.emit("print " + variavel.addr());
        } else if (t == TipoToken.NUM_INT || t == TipoToken.NUM_DEC) {
            codigo.emit("print " + constante().addr());
        } else if (t == TipoToken.TEXTO) {
            codigo.emit("print " + tokenAtual().getLexema());
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
    // Equivale a S → if ( B ) M1 S1 [N else M2 S2]
    private List<Integer> selecao() {
        int linha = tokenAtual().getLinha();
        consome(TipoToken.IF);
        consome(TipoToken.ABRE_PAR);
        Expressao b = condicao(linha, expressao());
        consome(TipoToken.FECHA_PAR);
        codigo.backpatch(b.truelist(), codigo.nextinstr()); // M1: verdadeiro entra no if
        bloco();
        return senao(b);
    }

    // Regra: SENÃO → else BLOCO | ε
    private List<Integer> senao(Expressao b) {
        if (tokenAtual().getTipo() == TipoToken.ELSE) {
            consome(TipoToken.ELSE);
            List<Integer> n = makelist(codigo.emit("goto _")); // N: fim do if pula o else
            codigo.backpatch(b.falselist(), codigo.nextinstr()); // M2: falso entra no else
            bloco();
            return n;
        }
        return b.falselist(); // sem else, o falso pula o if
    }

    // Regra: ENQUANTO → while (EXPRESSÃO) BLOCO
    // Equivale a S → while M1 ( B ) M2 S1
    private List<Integer> enquanto() {
        int linha = tokenAtual().getLinha();
        consome(TipoToken.WHILE);
        int m1 = codigo.nextinstr(); // início do teste
        consome(TipoToken.ABRE_PAR);
        Expressao b = condicao(linha, expressao());
        consome(TipoToken.FECHA_PAR);
        codigo.backpatch(b.truelist(), codigo.nextinstr()); // M2: verdadeiro entra no corpo
        bloco();
        codigo.emit("goto " + m1); // volta para o teste
        return b.falselist();
    }

    // Regra: ATRIBUIÇÃO → id = COMPLEMENTO;
    private void atribuicao() {
        Token id = tokenAtual();
        consome(TipoToken.ID);
        consome(TipoToken.ATRIB);
        String tipo = semantico.tipoVariavel(id);
        Expressao valor = codigo.paraValor(complemento());
        semantico.verificarAtribuicao(id, tipo, valor.tipo());
        codigo.emit(id.getLexema() + " = " + valor.addr());
        consome(TipoToken.PONTO_VIRGULA);
    }

    // Regra: COMPLEMENTO → EXPRESSÃO | FUNÇÃO
    private Expressao complemento() {
        if (tokenAtual().getTipo() == TipoToken.FUNC) {
            return funcao();
        } else {
            return expressao();
        }
    }

    // Regra: FUNÇÃO → func id (ARGUMENTOS)
    // Chamada de função.
    private Expressao funcao() {
        consome(TipoToken.FUNC);
        Token id = tokenAtual();
        consome(TipoToken.ID);
        consome(TipoToken.ABRE_PAR);
        List<Expressao> argumentos = argumentos();
        consome(TipoToken.FECHA_PAR);

        List<String> tipos = argumentos.stream().map(Expressao::tipo).toList();
        String retorno = semantico.tipoChamada(id, tipos);
        return codigo.chamada(id.getLexema(), argumentos, retorno);
    }

    // Regra: ARGUMENTOS → EXPRESSÃO NOVO_ARGUMENTO | ε
    private List<Expressao> argumentos() {
        List<Expressao> argumentos = new ArrayList<>();
        if (tokenAtual().getTipo() != TipoToken.FECHA_PAR) {
            argumentos.add(codigo.paraValor(expressao()));
            novoArgumento(argumentos);
        }
        return argumentos;
    }

    // Regra: NOVO_ARGUMENTO → , EXPRESSÃO NOVO_ARGUMENTO | ε
    private void novoArgumento(List<Expressao> argumentos) {
        if (tokenAtual().getTipo() == TipoToken.VIRGULA) {
            consome(TipoToken.VIRGULA);
            argumentos.add(codigo.paraValor(expressao()));
            novoArgumento(argumentos);
        }
    }

    // Regra: RETORNO → return EXPRESSÃO;
    private void retorno() {
        int linha = tokenAtual().getLinha();
        consome(TipoToken.RETURN);
        Expressao valor = codigo.paraValor(expressao());
        semantico.verificarRetorno(linha, valor.tipo());
        codigo.emit("return " + valor.addr());
        consome(TipoToken.PONTO_VIRGULA);
    }

    // Regra: EXPRESSÃO → EXPR_OU
    private Expressao expressao() {
        return exprOu();
    }

    // Regra: EXPR_OU → EXPR_E EXPR_OU2
    private Expressao exprOu() {
        return exprOu2(exprE());
    }

    // Regra: EXPR_OU2 → || EXPR_E EXPR_OU2 | ε
    private Expressao exprOu2(Expressao esquerda) {
        if (tokenAtual().getTipo() == TipoToken.OR) {
            int linha = tokenAtual().getLinha();
            consome(TipoToken.OR);
            Expressao b1 = condicao(linha, esquerda);
            int m = codigo.nextinstr();
            Expressao b2 = condicao(linha, exprE());
            return exprOu2(codigo.ou(b1, m, b2));
        }
        return esquerda;
    }

    // Regra: EXPR_E → EXPR_RELACIONAL EXPR_E2
    private Expressao exprE() {
        return exprE2(exprRelacional());
    }

    // Regra: EXPR_E2 → && EXPR_RELACIONAL EXPR_E2 | ε
    private Expressao exprE2(Expressao esquerda) {
        if (tokenAtual().getTipo() == TipoToken.AND) {
            int linha = tokenAtual().getLinha();
            consome(TipoToken.AND);
            Expressao b1 = condicao(linha, esquerda);
            int m = codigo.nextinstr();
            Expressao b2 = condicao(linha, exprRelacional());
            return exprE2(codigo.e(b1, m, b2));
        }
        return esquerda;
    }

    // Regra: EXPR_RELACIONAL → EXPR_ADITIVA EXPR_RELACIONAL2
    private Expressao exprRelacional() {
        return exprRelacional2(exprAditiva());
    }

    // Regra: EXPR_RELACIONAL2 → comp EXPR_ADITIVA | ε
    private Expressao exprRelacional2(Expressao esquerda) {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.MAIOR || t == TipoToken.MAIOR_IGUAL ||
            t == TipoToken.MENOR || t == TipoToken.MENOR_IGUAL ||
            t == TipoToken.DIFERENTE || t == TipoToken.IGUAL) {
            Token op = tokenAtual();
            consome(t);
            Expressao e1 = codigo.paraValor(esquerda);
            Expressao e2 = codigo.paraValor(exprAditiva());
            semantico.verificarComparacao(op, e1.tipo(), e2.tipo());
            return codigo.relacional(e1, op.getLexema(), e2);
        }
        return esquerda;
    }

    // Regra: EXPR_ADITIVA → EXPR_MULTIPLICATIVA EXPR_ADITIVA2
    private Expressao exprAditiva() {
        return exprAditiva2(exprMultiplicativa());
    }

    // Regra: EXPR_ADITIVA2 → OP_ADITIVO EXPR_MULTIPLICATIVA EXPR_ADITIVA2 | ε
    private Expressao exprAditiva2(Expressao esquerda) {
        if (tokenAtual().getTipo() == TipoToken.SOMA || tokenAtual().getTipo() == TipoToken.SUB) {
            Token op = tokenAtual();
            opAditivo();
            return exprAditiva2(aritmetica(esquerda, op, exprMultiplicativa()));
        }
        return esquerda;
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
    private Expressao exprMultiplicativa() {
        return exprMultiplicativa2(fator());
    }

    // Regra: EXPR_MULTIPLICATIVA2 → OP_MULTIPLICATIVO FATOR EXPR_MULTIPLICATIVA2 | ε
    private Expressao exprMultiplicativa2(Expressao esquerda) {
        TipoToken t = tokenAtual().getTipo();
        if (t == TipoToken.MULT || t == TipoToken.DIV || t == TipoToken.MOD) {
            Token op = tokenAtual();
            opMultiplicativo();
            return exprMultiplicativa2(aritmetica(esquerda, op, fator()));
        }
        return esquerda;
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
    private Expressao fator() {
        TipoToken t = tokenAtual().getTipo();

        if (t == TipoToken.SOMA || t == TipoToken.SUB || t == TipoToken.ID || t == TipoToken.NUM_INT || t == TipoToken.NUM_DEC) {
            Token sinal = tokenAtual();
            sinal(); // pode ser ε
            Expressao termo = termo();
            if (t == TipoToken.SOMA || t == TipoToken.SUB) {
                semantico.verificarSinal(sinal, termo.tipo());
            }
            return t == TipoToken.SUB ? codigo.menos(termo) : termo;
        } else if (t == TipoToken.TEXTO) {
            Expressao texto = Expressao.valor("texto", tokenAtual().getLexema());
            consome(TipoToken.TEXTO);
            return texto;
        } else if (t == TipoToken.NOT) {
            int linha = tokenAtual().getLinha();
            consome(TipoToken.NOT);
            return codigo.nao(condicao(linha, fator()));
        } else if (t == TipoToken.ABRE_PAR) {
            consome(TipoToken.ABRE_PAR);
            Expressao e = expressao();
            consome(TipoToken.FECHA_PAR);
            return e;
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Fator inválido. Token inesperado: " + t);
        }
    }

    // Regra: TERMO → id DIMENSAO2 | CONSTANTE
    private Expressao termo() {
        if (tokenAtual().getTipo() == TipoToken.ID) {
            Token id = tokenAtual();
            consome(TipoToken.ID);
            return variavel(id);
        } else {
            return constante();
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
    private Expressao constante() {
        Token numero = tokenAtual();
        if (numero.getTipo() == TipoToken.NUM_INT) {
            consome(TipoToken.NUM_INT);
            return Expressao.valor("int", numero.getLexema());
        } else if (numero.getTipo() == TipoToken.NUM_DEC) {
            consome(TipoToken.NUM_DEC);
            return Expressao.valor("float", numero.getLexema());
        } else {
            throw new RuntimeException("Erro Sintático na linha " + tokenAtual().getLinha() + ": Esperado constante numérica");
        }
    }

    // ---------------- auxiliares ----------------

    // id DIMENSAO2: a variável precisa estar declarada. Ex: z[i][j] tem tipo int e addr "z[i][j]"
    private Expressao variavel(Token id) {
        String tipo = semantico.tipoVariavel(id);
        List<String> indices = dimensao2();
        return Expressao.valor(semantico.tipoElemento(id, tipo, indices.size()), id.getLexema() + String.join("", indices));
    }

    // + - * / %: o semântico confere os tipos e o gerador emite t = a op b
    private Expressao aritmetica(Expressao e1, Token op, Expressao e2) {
        String tipo = semantico.tipoAritmetica(op, e1.tipo(), e2.tipo());
        return codigo.operacao(tipo, e1, op.getLexema(), e2);
    }

    // usada em if, while, &&, || e !: precisa ser boolean e vira forma de salto
    private Expressao condicao(int linha, Expressao e) {
        semantico.verificarCondicao(linha, e.tipo());
        return codigo.paraSalto(e);
    }
}
