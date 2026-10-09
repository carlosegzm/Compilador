package br.edu.ifsc.compiladores;

import br.edu.ifsc.compiladores.lexico.AnalisadorLexico;
import br.edu.ifsc.compiladores.lexico.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        String caminhoArquivo = "teste.txt";

        if (args.length > 0) {
            caminhoArquivo = args[0]; // pode passar outro arquivo se quiser
        }
        try {
            System.out.println("=== INICIANDO ANALISE LEXICA ===");
            System.out.println("Lendo o arquivo: " + caminhoArquivo + "\n");

            AnalisadorLexico lexico = new AnalisadorLexico(caminhoArquivo);
            List<Token> tokens = lexico.analisar();

            for (Token t : tokens) {
                System.out.println(t);
            }

            // TODO: analise sintática

        } catch (Exception e) {
            System.err.println("\n[X] Erro durante a compilacao:");
            System.err.println(e.getMessage());
        }

    }
}