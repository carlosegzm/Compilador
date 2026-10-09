package br.edu.ifsc.compiladores.lexico;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorLexico {

    private String codigoFonte;
    private int posicao;
    private int linha;
    private int coluna;

    public AnalisadorLexico(String caminhoArquivo) throws IOException {
        this.codigoFonte = new String(Files.readAllBytes(Paths.get(caminhoArquivo)));
        this.posicao = 0;
        this.linha = 1;
        this.coluna = 1;
    }

    // le o proximo caractere e atualiza linha e coluna
    private char lerCaractere() {
        if (isEOF()) return '\0';
        char c = codigoFonte.charAt(posicao++);
        if (c == '\n') {
            linha++;
            coluna = 1;
        } else {
            coluna++;
        }
        return c;
    }

    // olha o próximo caractere sem avançar a posicao
    private char verProximoCaractere() {
        if (isEOF()) return '\0';
        return codigoFonte.charAt(posicao);
    }

    // função aux p verificar se é letra
    private boolean ehLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // função aux p verificar se é numero
    private boolean ehDigito(char c) {
        return (c >= '0' && c <= '9');
    }

    // verifica se é o fim do arquivo
    private boolean isEOF() {
        return posicao >= codigoFonte.length();
    }

    // método principal
    public List<Token> analisar() {
        List<Token> tokens = new ArrayList<>();

        while (!isEOF()) {
            char c = lerCaractere();

            // ignora espaços em branco, tabulacoes e quebras de linha
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                continue;
            }

            // ignora comentários
            if (c == '/' && verProximoCaractere() == '/') {
                lerCaractere(); // consome o segundo /
                while (!isEOF() && verProximoCaractere() != '\n') {
                    lerCaractere(); // le tudo até o fim da linha
                }
                continue;
            }

            int linhaInicial = linha;
            int colunaInicial = coluna - 1;

            // tratamento de texto ""
            if (c == '"') {
                StringBuilder lexema = new StringBuilder();
                lexema.append(c);
                while (!isEOF() && verProximoCaractere() != '"') {
                    lexema.append(lerCaractere());
                }
                if (!isEOF()) {
                    lexema.append(lerCaractere()); //consome " de fechamento
                }
                tokens.add(new Token(TipoToken.TEXTO, lexema.toString(), linhaInicial, colunaInicial));
                continue;
            }

            // tratamento de números
            if (ehDigito(c)) {
                StringBuilder lexema = new StringBuilder();
                lexema.append(c);
                boolean ehDecimal = false;

                while (!isEOF() && (ehDigito(verProximoCaractere()) || verProximoCaractere() == '.')) {
                    char proximo = lerCaractere();
                    if (proximo == '.') {
                        if (ehDecimal) {
                            throw new RuntimeException("Erro léxico: Ponto extra em número na linha " + linhaInicial);
                        }
                        ehDecimal = true;
                    }
                    lexema.append(proximo);
                }

                TipoToken tipo = ehDecimal ? TipoToken.NUM_DEC : TipoToken.NUM_INT;
                tokens.add(new Token(tipo, lexema.toString(), linhaInicial, colunaInicial));
                continue;
            }

            // tratamento de id e palavras reservadas
            if (ehLetra(c) || c == '_') {
                StringBuilder lexema = new StringBuilder();
                lexema.append(c);

                while (!isEOF()) {
                    char proximo = verProximoCaractere();

                    if (ehLetra(proximo) || ehDigito(proximo) || proximo == '_') {
                        lexema.append(lerCaractere());
                    } else {
                        break;
                    }
                }

                String palavra = lexema.toString();
                TipoToken tipo = verificarPalavraReservada(palavra);
                tokens.add(new Token(tipo, palavra, linhaInicial, colunaInicial));
                continue;
            }

            // operadores duplos ( &&, ||, ==, !=, >=, <= )
            if (c == '&' && verProximoCaractere() == '&') {
                lerCaractere();
                tokens.add(new Token(TipoToken.AND, "&&", linhaInicial, colunaInicial));
                continue;
            }
            if (c == '|' && verProximoCaractere() == '|') {
                lerCaractere();
                tokens.add(new Token(TipoToken.OR, "||", linhaInicial, colunaInicial));
                continue;
            }
            if (c == '=') {
                if (verProximoCaractere() == '=') {
                    lerCaractere();
                    tokens.add(new Token(TipoToken.IGUAL, "==", linhaInicial, colunaInicial));
                } else {
                    tokens.add(new Token(TipoToken.ATRIB, "=", linhaInicial, colunaInicial));
                }
                continue;
            }
            if (c == '!') {
                if (verProximoCaractere() == '=') {
                    lerCaractere();
                    tokens.add(new Token(TipoToken.DIFERENTE, "!=", linhaInicial, colunaInicial));
                } else {
                    tokens.add(new Token(TipoToken.NOT, "!", linhaInicial, colunaInicial));
                }
                continue;
            }
            if (c == '>') {
                if (verProximoCaractere() == '=') {
                    lerCaractere();
                    tokens.add(new Token(TipoToken.MAIOR_IGUAL, ">=", linhaInicial, colunaInicial));
                } else {
                    tokens.add(new Token(TipoToken.MAIOR, ">", linhaInicial, colunaInicial));
                }
                continue;
            }
            if (c == '<') {
                if (verProximoCaractere() == '=') {
                    lerCaractere();
                    tokens.add(new Token(TipoToken.MENOR_IGUAL, "<=", linhaInicial, colunaInicial));
                } else {
                    tokens.add(new Token(TipoToken.MENOR, "<", linhaInicial, colunaInicial));
                }
                continue;
            }

            // operadores simples e símbolos
            switch (c) {
                case '+': tokens.add(new Token(TipoToken.SOMA, "+", linhaInicial, colunaInicial)); break;
                case '-': tokens.add(new Token(TipoToken.SUB, "-", linhaInicial, colunaInicial)); break;
                case '*': tokens.add(new Token(TipoToken.MULT, "*", linhaInicial, colunaInicial)); break;
                case '/': tokens.add(new Token(TipoToken.DIV, "/", linhaInicial, colunaInicial)); break;
                case '%': tokens.add(new Token(TipoToken.MOD, "%", linhaInicial, colunaInicial)); break;
                case '(': tokens.add(new Token(TipoToken.ABRE_PAR, "(", linhaInicial, colunaInicial)); break;
                case ')': tokens.add(new Token(TipoToken.FECHA_PAR, ")", linhaInicial, colunaInicial)); break;
                case '[': tokens.add(new Token(TipoToken.ABRE_COL, "[", linhaInicial, colunaInicial)); break;
                case ']': tokens.add(new Token(TipoToken.FECHA_COL, "]", linhaInicial, colunaInicial)); break;
                case '{': tokens.add(new Token(TipoToken.ABRE_CHAVE, "{", linhaInicial, colunaInicial)); break;
                case '}': tokens.add(new Token(TipoToken.FECHA_CHAVE, "}", linhaInicial, colunaInicial)); break;
                case ',': tokens.add(new Token(TipoToken.VIRGULA, ",", linhaInicial, colunaInicial)); break;
                case ';': tokens.add(new Token(TipoToken.PONTO_VIRGULA, ";", linhaInicial, colunaInicial)); break;
                default:
                    throw new RuntimeException("Erro léxico: Caractere inesperado '" + c + "' na linha " + linhaInicial + ", coluna " + colunaInicial);
            }
        }

        // add um token eof p facilitar o analisador sintático
        tokens.add(new Token(TipoToken.EOF, "EOF", linha, coluna));
        return tokens;
    }

    // método p verificar se é id ou pr
    private TipoToken verificarPalavraReservada(String palavra) {
        switch (palavra) {
            case "int": return TipoToken.INT;
            case "float": return TipoToken.FLOAT;
            case "char": return TipoToken.CHAR;
            case "boolean": return TipoToken.BOOLEAN;
            case "void": return TipoToken.VOID;
            case "if": return TipoToken.IF;
            case "else": return TipoToken.ELSE;
            case "for": return TipoToken.FOR;
            case "while": return TipoToken.WHILE;
            case "scanf": return TipoToken.SCANF;
            case "println": return TipoToken.PRINTLN;
            case "main": return TipoToken.MAIN;
            case "return": return TipoToken.RETURN;
            case "func": return TipoToken.FUNC;
            default: return TipoToken.ID; // se nao é pr ent é id
        }
    }


}
