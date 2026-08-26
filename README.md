# FTC Verificador PDF

Aplicacao Java para cruzar contas encontradas em PDFs com uma planilha Excel, atualizar o status de cada conta e copiar os PDFs para uma pasta de saida organizada por `ID`.

## O que o projeto faz

1. Le todos os arquivos PDF da pasta configurada em `pdf.entrada.dir` (inclui subpastas).
2. Extrai dos PDFs:
   - `Conta`;
   - `NFCom`;
   - `Tipo`;
   - `Finalidade`.
3. Compara as contas extraidas com a coluna `Conta` da planilha.
4. Atualiza a planilha com as colunas de saida na ordem:
   - `Arquivo Existe?`
   - `NFCom`
   - `Tipo`
   - `Finalidade`
   - `Caminho Arquivos`
5. Copia os PDFs encontrados para `pdf.saida.dir/<ID>`, renomeando para:
   - `[numero_conta] - [nome_original_do_arquivo].pdf`

## Requisitos

- Java 11+
- Maven 3.8+

## Configuracao

Pode editar o arquivo `src/main/resources/app.properties` com o caminho dos arquivos PDFs e XLS:

```properties
pdf.entrada.dir=arquivos/pdfs/in
pdf.saida.dir=arquivos/pdfs/out
xls.dir=arquivos/xls
```

### Regras das configuracoes

- `pdf.entrada.dir`: pasta de entrada dos PDFs.
- `pdf.saida.dir`: pasta raiz para copia dos PDFs por `ID`.
- `xls.dir`:
  - pode ser caminho direto para um arquivo `.xlsx`, ou
  - uma pasta com exatamente um arquivo `.xlsx`.

Observacoes:
- Se `pdf.entrada.dir` ou `pdf.saida.dir` nao existirem, o sistema cria automaticamente.
- Se `pdf.entrada.dir` ficar sem PDFs, a execucao falha com erro informando que nenhum PDF foi encontrado.

## Formato esperado da planilha

A primeira aba da planilha deve conter no cabecalho:

- `ID` (obrigatoria)
- `Conta` (obrigatoria)

Colunas de saida (criadas/atualizadas automaticamente), sempre nesta ordem:

- `Arquivo Existe?`
- `NFCom`
- `Tipo`
- `Finalidade`
- `Caminho Arquivos`

### Como preencher `Conta`

- Pode ter uma ou varias contas por linha.
- Quando houver varias contas, separar por quebra de linha na mesma celula.
- A comparacao de conta ignora:
  - caracteres nao numericos;
  - zeros a esquerda.

Exemplo:
- `8890327-0005` casa com `00008890327-0005`.

## Regras de extracao das novas colunas

- `NFCom`: usa somente a parte numerica da linha que comeca com `NFCOM Nº`.
- `Tipo` e `Finalidade`: usa somente os numeros da linha no padrao `TIPO [numeroTipo] | FINALIDADE [numeroFinalidade]`.
- Se a conta nao for encontrada para a linha da planilha, preencher `NFCom`, `Tipo` e `Finalidade` com `NA`.

## Como executar

No diretorio raiz do projeto:

```bash
mvn clean compile exec:java -Dexec.mainClass=com.ftcverificador.Main
```

## Estrutura de saida (exemplo)

```text
arquivos/
  pdfs/
    out/
      251120000041/
        1234567890 - fatura_cliente_a.pdf
        9876543210 - fatura_cliente_b.pdf
```

## Comportamento das colunas de saida

- `Arquivo Existe?`: `Sim` ou `Nao` por conta.
- `NFCom`, `Tipo`, `Finalidade`: valor numerico por conta quando encontrada, ou `NA` quando nao encontrada.
- `Caminho Arquivos`:
  - se nenhuma conta da linha for encontrada: `Nenhum arquivo encontrado`;
  - se uma ou mais contas da linha forem encontradas: caminho da pasta do `ID` (somente uma vez por linha).

## Erros comuns

- `Coluna obrigatoria nao encontrada: ID`: a planilha nao tem a coluna `ID`.
- `Nenhum arquivo .xlsx encontrado`: `xls.dir` aponta para pasta sem planilha.
- `Mais de um arquivo .xlsx encontrado`: `xls.dir` aponta para pasta com mais de um `.xlsx`.
- `Nenhum arquivo PDF encontrado`: nao ha PDFs na pasta de entrada.
