package br.edu.ifsc.compiladores.lexico;

public class Token {

    private TipoToken tipo;
    private String lexema;
    private int linha;
    private int coluna;

    public Token(TipoToken tipo, String lexema, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
    }

    public TipoToken getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinha() { return linha; }
    public int getColuna() { return coluna; }

    @Override
    public String toString() {
        return String.format("[ %-16s ]   Lexema: '%-15s'   |   Linha: %-3d   |   Coluna: %-3d", tipo, lexema, linha, coluna);
    }


}
