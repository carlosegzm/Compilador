# Testes

Os testes são arquivos `.txt` com código-fonte do C simplificado. Cada arquivo passa
pelo analisador léxico e depois pelo sintático, e o resultado obtido é comparado
com o resultado esperado, que é definido **pelo prefixo do nome do arquivo**.

## Nomenclatura

| Prefixo   | Significado                       | Resultado esperado |
|-----------|-----------------------------------|--------------------|
| `ok_`     | Código válido                     | `OK`               |
| `lex_`    | Deve falhar na análise léxica     | `ERRO_LEXICO`      |
| `sint_`   | Deve falhar na análise sintática  | `ERRO_SINTATICO`   |
| *(outro)* | Sem veredito, apenas executado    | `?`                |

Exemplos:

```
tests/
├── ok_soma.txt
├── ok_while_aninhado.txt
├── lex_caractere_invalido.txt
├── lex_string_aberta.txt
├── sint_falta_ponto_virgula.txt
└── sint_falta_chave.txt
```

> Dica: use nomes descritivos depois do prefixo. O nome aparece na saída e
> deve dizer qual erro o teste procura.

## Como o resultado é calculado

1. **Léxico**: se lançar exceção (ou gerar token de erro), o resultado é `ERRO_LEXICO`
   e o sintático nem roda.
2. **Sintático**: se o léxico passou e o sintático lançar exceção, o resultado é `ERRO_SINTATICO`.
3. Se nenhum dos dois falhar, o resultado é `OK` 