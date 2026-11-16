package br.edu.ifsc.compiladores.intermediario;

import java.util.ArrayList;
import java.util.List;

/**
 * GeradorCodigo
 *
 * <p>
 *      Gera o código de três endereços, com as instruções numeradas a partir de 1.
 *      As funções básicas têm os mesmos nomes do esquema de tradução da aula:
 *      newTemp, nextinstr, emit, makelist, merge e backpatch.
 * <p>
 */
public class GeradorCodigo {

    private final List<String> instrucoes = new ArrayList<>();
    private int contadorTemp = 0;

    // ---------------- funções do esquema de tradução ----------------

    // cria um novo temporário: t1, t2, t3...
    public String newTemp() {
        contadorTemp++;
        return "t" + contadorTemp;
    }

    // número da próxima instrução que será emitida
    public int nextinstr() {
        return instrucoes.size() + 1;
    }

    // adiciona uma instrução e retorna o número dela
    public int emit(String instrucao) {
        instrucoes.add(instrucao);
        return instrucoes.size();
    }

    public static List<Integer> makelist(int instrucao) {
        List<Integer> lista = new ArrayList<>();
        lista.add(instrucao);
        return lista;
    }

    public static List<Integer> merge(List<Integer> a, List<Integer> b) {
        List<Integer> lista = new ArrayList<>(a);
        lista.addAll(b);
        return lista;
    }

    // troca o "_" dos gotos pendentes da lista pelo número da instrução de destino
    public void backpatch(List<Integer> lista, int destino) {
        for (int i : lista) {
            instrucoes.set(i - 1, instrucoes.get(i - 1).replace("goto _", "goto " + destino));
        }
    }

    // ---------------- expressões ----------------

    // E → E1 op E2
    public Expressao operacao(String tipo, Expressao e1, String op, Expressao e2) {
        String temp = newTemp();
        emit(temp + " = " + e1.addr() + " " + op + " " + e2.addr());
        return Expressao.valor(tipo, temp);
    }

    // E → - E1
    public Expressao menos(Expressao e) {
        String temp = newTemp();
        emit(temp + " = minus " + e.addr());
        return Expressao.valor(e.tipo(), temp);
    }

    // chamada de função: um param por argumento e depois o call
    public Expressao chamada(String nome, List<Expressao> argumentos, String retorno) {
        for (Expressao argumento : argumentos) {
            emit("param " + argumento.addr());
        }
        String call = "call " + nome + ", " + argumentos.size();
        if (retorno.equals("void")) {
            emit(call);
            return Expressao.valor("void", null);
        }
        String temp = newTemp();
        emit(temp + " = " + call);
        return Expressao.valor(retorno, temp);
    }

    // ---------------- condições (curto-circuito) ----------------

    // B → E1 rel E2
    public Expressao relacional(Expressao e1, String op, Expressao e2) {
        int i = emit("if " + e1.addr() + " " + op + " " + e2.addr() + " goto _");
        emit("goto _");
        return Expressao.salto(makelist(i), makelist(i + 1));
    }

    // B → B1 || M B2: se B1 for falso, testa B2
    public Expressao ou(Expressao b1, int m, Expressao b2) {
        backpatch(b1.falselist(), m);
        return Expressao.salto(merge(b1.truelist(), b2.truelist()), b2.falselist());
    }

    // B → B1 && M B2: se B1 for verdadeiro, testa B2
    public Expressao e(Expressao b1, int m, Expressao b2) {
        backpatch(b1.truelist(), m);
        return Expressao.salto(b2.truelist(), merge(b1.falselist(), b2.falselist()));
    }

    // B → ! B1: troca a truelist com a falselist
    public Expressao nao(Expressao b) {
        return Expressao.salto(b.falselist(), b.truelist());
    }

    // usa um valor boolean (ex: uma variável) como condição: if x goto _
    public Expressao paraSalto(Expressao e) {
        if (e.ehSalto()) {
            return e;
        }
        int i = emit("if " + e.addr() + " goto _");
        emit("goto _");
        return Expressao.salto(makelist(i), makelist(i + 1));
    }

    // usa uma condição como valor (ex: b = x > y): guarda true ou false em um temporário
    public Expressao paraValor(Expressao e) {
        if (!e.ehSalto()) {
            return e;
        }
        String temp = newTemp();
        int k = nextinstr();
        backpatch(e.truelist(), k);
        emit(temp + " = true");
        emit("goto " + (k + 3));
        backpatch(e.falselist(), k + 2);
        emit(temp + " = false");
        return Expressao.valor("boolean", temp);
    }

    // código numerado, uma instrução por linha
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < instrucoes.size(); i++) {
            sb.append(String.format("%3d: %s%n", i + 1, instrucoes.get(i)));
        }
        return sb.toString();
    }
}
