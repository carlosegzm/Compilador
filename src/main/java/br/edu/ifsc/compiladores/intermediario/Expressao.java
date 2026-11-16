package br.edu.ifsc.compiladores.intermediario;

import java.util.List;

/**
 * Expressao
 *
 * <p>
 *      Atributos de uma expressão, como no esquema de tradução da aula. Ela pode ser:
 *      - valor: o resultado está em addr (variável, constante ou temporário);
 *      - salto: uma condição em curto-circuito, com os gotos pendentes em truelist e falselist.
 * <p>
 */
public record Expressao(String tipo, String addr, List<Integer> truelist, List<Integer> falselist) {

    public static Expressao valor(String tipo, String addr) {
        return new Expressao(tipo, addr, null, null);
    }

    // toda condição em forma de salto é boolean
    public static Expressao salto(List<Integer> truelist, List<Integer> falselist) {
        return new Expressao("boolean", null, truelist, falselist);
    }

    public boolean ehSalto() {
        return truelist != null;
    }
}
