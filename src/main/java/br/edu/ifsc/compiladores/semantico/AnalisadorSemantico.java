package br.edu.ifsc.compiladores.semantico;

import br.edu.ifsc.compiladores.lexico.TipoToken;
import br.edu.ifsc.compiladores.lexico.Token;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AnalisadorSemantico
 *
 * <p>
 *      Guarda a tabela de símbolos e confere as regras de tipo da linguagem.
 *      É chamado pelo analisador sintático durante a análise (tradução dirigida pela sintaxe).
 *      Os tipos são texto: "int", "float", "char", "boolean", "void", "texto"
 *      e vetores com as dimensões, ex: "int[2][3]".
 * <p>
 */
public class AnalisadorSemantico {

    // erro semântico, separado para o Main diferenciar do erro sintático
    public static class Erro extends RuntimeException {
        public Erro(int linha, String mensagem) {
            super("Erro Semântico na linha " + linha + ": " + mensagem);
        }
    }

    private record Funcao(String retorno, List<String> parametros) {}

    // tabela de símbolos. A chave da variável é "funcao.nome", então cada função tem o seu escopo
    private final Map<String, String> variaveis = new LinkedHashMap<>();
    private final Map<String, Funcao> funcoes = new LinkedHashMap<>();
    private String funcaoAtual;

    // ---------------- tabela de símbolos ----------------

    public void iniciarFuncao(String nome) {
        funcaoAtual = nome;
    }

    public void declararFuncao(Token id, String retorno, List<String> parametros) {
        if (funcoes.containsKey(id.getLexema())) {
            throw new Erro(id.getLinha(), "função '" + id.getLexema() + "' já foi declarada");
        }
        funcoes.put(id.getLexema(), new Funcao(retorno, parametros));
    }

    public void declararVariavel(Token id, String tipo) {
        String chave = funcaoAtual + "." + id.getLexema();
        if (variaveis.containsKey(chave)) {
            throw new Erro(id.getLinha(), "'" + id.getLexema() + "' já foi declarada");
        }
        variaveis.put(chave, tipo);
    }

    // tipo da variável. Erro se ela não foi declarada nesta função
    public String tipoVariavel(Token id) {
        String tipo = variaveis.get(funcaoAtual + "." + id.getLexema());
        if (tipo == null) {
            throw new Erro(id.getLinha(), "variável '" + id.getLexema() + "' não foi declarada");
        }
        return tipo;
    }

    // ---------------- regras de tipo ----------------

    public void verificarTamanho(Token tamanho) {
        if (Integer.parseInt(tamanho.getLexema()) <= 0) {
            throw new Erro(tamanho.getLinha(), "tamanho de vetor deve ser maior que zero");
        }
    }

    // tipo que sobra depois dos índices. Ex: "int[2][3]" com 1 índice vira "int[3]", com 2 vira "int"
    public String tipoElemento(Token id, String tipo, int indices) {
        for (int i = 0; i < indices; i++) {
            if (!tipo.contains("[")) {
                throw new Erro(id.getLinha(), "'" + id.getLexema() + "' foi acessado com índices demais");
            }
            tipo = tipo.substring(0, tipo.indexOf('[')) + tipo.substring(tipo.indexOf(']') + 1);
        }
        return tipo;
    }

    // scanf e println trabalham com um valor só, não com o vetor inteiro
    public void verificarNaoVetor(Token id, String tipo) {
        if (tipo.contains("[")) {
            throw new Erro(id.getLinha(), "vetor '" + id.getLexema() + "' precisa ser usado com índices");
        }
    }

    public void verificarIndice(int linha, String tipo) {
        if (!tipo.equals("int")) {
            throw new Erro(linha, "índice de vetor deve ser int, mas é " + tipo);
        }
    }

    public void verificarCondicao(int linha, String tipo) {
        if (!tipo.equals("boolean")) {
            throw new Erro(linha, "condição deve ser boolean, mas é " + tipo);
        }
    }

    // atribuição estrita: os tipos precisam ser iguais
    public void verificarAtribuicao(Token id, String tipoVariavel, String tipoValor) {
        if (!tipoVariavel.equals(tipoValor)) {
            throw new Erro(id.getLinha(), "não é possível atribuir " + tipoValor
                    + " à variável '" + id.getLexema() + "' do tipo " + tipoVariavel);
        }
    }

    // + - * / %: só números, e % só int. O resultado é float se algum lado for float
    public String tipoAritmetica(Token op, String t1, String t2) {
        if (!ehNumero(t1) || !ehNumero(t2)) {
            throw new Erro(op.getLinha(), "operador '" + op.getLexema() + "' precisa de números, mas recebeu " + t1 + " e " + t2);
        }
        if (op.getTipo() == TipoToken.MOD && !(t1.equals("int") && t2.equals("int"))) {
            throw new Erro(op.getLinha(), "operador '%' só aceita int");
        }
        return (t1.equals("float") || t2.equals("float")) ? "float" : "int";
    }

    public void verificarSinal(Token sinal, String tipo) {
        if (!ehNumero(tipo)) {
            throw new Erro(sinal.getLinha(), "sinal '" + sinal.getLexema() + "' só pode ser usado com número, mas recebeu " + tipo);
        }
    }

    // < > <= >= comparam números. == e != também comparam dois boolean ou dois char
    public void verificarComparacao(Token op, String t1, String t2) {
        boolean numeros = ehNumero(t1) && ehNumero(t2);
        boolean igualdade = op.getTipo() == TipoToken.IGUAL || op.getTipo() == TipoToken.DIFERENTE;
        boolean mesmoTipo = t1.equals(t2) && (t1.equals("boolean") || t1.equals("char"));
        if (!numeros && !(igualdade && mesmoTipo)) {
            throw new Erro(op.getLinha(), "não é possível comparar " + t1 + " " + op.getLexema() + " " + t2);
        }
    }

    // a função precisa existir e os argumentos precisam bater com os parâmetros. Retorna o tipo de retorno
    public String tipoChamada(Token id, List<String> argumentos) {
        Funcao funcao = funcoes.get(id.getLexema());
        if (funcao == null) {
            throw new Erro(id.getLinha(), "função '" + id.getLexema() + "' não foi declarada");
        }
        if (!funcao.parametros().equals(argumentos)) {
            throw new Erro(id.getLinha(), "função '" + id.getLexema() + "' espera " + funcao.parametros()
                    + ", mas recebeu " + argumentos);
        }
        return funcao.retorno();
    }

    // o return precisa estar dentro de uma função e devolver o tipo dela
    public void verificarRetorno(int linha, String tipo) {
        Funcao funcao = funcoes.get(funcaoAtual);
        if (funcao == null) {
            throw new Erro(linha, "return só pode ser usado dentro de uma função");
        }
        if (!funcao.retorno().equals(tipo)) {
            throw new Erro(linha, "função '" + funcaoAtual + "' deve retornar " + funcao.retorno() + ", mas retorna " + tipo);
        }
    }

    private boolean ehNumero(String tipo) {
        return tipo.equals("int") || tipo.equals("float");
    }

    // tabela de símbolos, exibida no modo -v
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        funcoes.forEach((nome, f) -> sb.append(String.format("[ FUNCAO   ]   %-12s |   retorno: %-8s |   parametros: %s%n",
                nome, f.retorno(), f.parametros())));
        variaveis.forEach((nome, tipo) -> sb.append(String.format("[ VARIAVEL ]   %-12s |   tipo: %s%n", nome, tipo)));
        return sb.toString();
    }
}
