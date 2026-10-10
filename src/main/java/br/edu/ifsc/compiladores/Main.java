package br.edu.ifsc.compiladores;

import br.edu.ifsc.compiladores.lexico.AnalisadorLexico;
import br.edu.ifsc.compiladores.lexico.Token;
import br.edu.ifsc.compiladores.sintatico.AnalisadorSintatico;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Main {
enum Resultado { OK, ERRO_LEXICO, ERRO_SINTATICO }

    // cores ANSI 
    static final String VERDE = "\u001B[32m", 
            VERMELHO = "\u001B[31m", 
            CINZA = "\u001B[90m", 
            RESET = "\u001B[0m";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Uso: java -cp <jar> br.edu.ifsc.compiladores.Main [-v] <arquivo|pasta> ...");
            return;
        }

        boolean verbose = false;
        List<Path> arquivos = new ArrayList<>();
        for (String a : args) {
            if (a.equals("-v")) verbose = true;
            else coletar(Path.of(a), arquivos);
        }

        int passou = 0, falhou = 0;

        for (Path arq : arquivos) {
            StringBuilder detalhe = new StringBuilder();
            Resultado obtido = executar(arq, verbose, detalhe);
            Resultado esperado = esperadoPeloNome(arq);

            String status;
            if (esperado == null) {
                status = CINZA + "[ -- ]" + RESET;
            } else if (esperado == obtido) {
                status = VERDE + "[PASS]" + RESET; passou++;
            } else {
                status = VERMELHO + "[FAIL]" + RESET; falhou++;
            }

            System.out.printf("%s %-35s esperado=%-14s obtido=%s%n",
                    status, arq.getFileName(),
                    esperado == null ? "?" : esperado, obtido);

            if (detalhe.length() > 0) {
                System.out.println("       " + CINZA + detalhe + RESET);
            }
        }

        System.out.printf("%n%d arquivos | %s%d passaram%s | %s%d falharam%s%n",
                arquivos.size(), VERDE, passou, RESET, VERMELHO, falhou, RESET);

        if (falhou > 0) System.exit(1); // util p/ CI e Makefile
    }

    static Resultado executar(Path arq, boolean verbose, StringBuilder detalhe) {
        List<Token> tokens;

        try {
            tokens = new AnalisadorLexico(arq.toString()).analisar();
        } catch (Exception e) {
            detalhe.append("lexico: ").append(e.getMessage());
            return Resultado.ERRO_LEXICO;
        }

        if (verbose) {
            System.out.println("--- tokens de " + arq.getFileName() + " ---");
            tokens.forEach(System.out::println);
        }

        try {
            new AnalisadorSintatico(tokens).analisar();
        } catch (Exception e) {
            detalhe.append("sintatico: ").append(e.getMessage());
            return Resultado.ERRO_SINTATICO;
        }

        return Resultado.OK;
    }

    static Resultado esperadoPeloNome(Path arq) {
        String nome = arq.getFileName().toString();
        if (nome.startsWith("ok_"))   return Resultado.OK;
        if (nome.startsWith("lex_"))  return Resultado.ERRO_LEXICO;
        if (nome.startsWith("sint_")) return Resultado.ERRO_SINTATICO;
        return null;
    }

    static void coletar(Path p, List<Path> destino) throws IOException {
        if (Files.isDirectory(p)) {
            try (Stream<Path> s = Files.list(p)) {
                s.filter(f -> f.toString().endsWith(".txt"))
                 .sorted()
                 .forEach(destino::add);
            }
        } else {
            destino.add(p);
        }
    }
}