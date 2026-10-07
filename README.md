# FTC Verificador PDF

Aplicação desktop desenvolvida em Java para auxiliar na análise, organização e validação de arquivos relacionados à NFCom.

A automação lê arquivos DANFE-COM em PDF, identifica contas e chaves de acesso NFCom, cruza essas informações com uma planilha de casos de teste e organiza os documentos automaticamente por CT/cenário.

Opcionalmente, também pode relacionar e copiar os respectivos XMLs utilizando a chave de acesso da NFCom.

---

## Funcionalidades

- Leitura de PDFs em pastas e subpastas.
- Leitura direta de arquivos `.zip` contendo PDFs.
- Extração automática dos PDFs do ZIP para uma pasta temporária.
- Exclusão automática dos arquivos temporários após o processamento.
- Identificação das contas presentes nos DANFE-COM.
- Identificação das chaves de acesso NFCom.
- Contagem de PDFs analisados.
- Contagem de DANFEs encontradas.
- Cruzamento das contas com uma planilha Excel.
- Identificação do cenário de teste através da coluna `Test name (initial)`.
- Suporte a uma mesma conta associada a múltiplos CTs.
- Organização automática dos PDFs por cenário de teste.
- Criação de uma pasta por conta dentro de cada cenário.
- Identificação de contas encontradas nos PDFs que não possuem CT/cenário cadastrado.
- Criação automática da pasta `Contas sem Cenário`.
- Renomeação dos PDFs sem cenário utilizando o número da conta.
- ZIP de XMLs opcional.
- Associação entre DANFE-COM e XML através da chave de acesso.
- Renomeação automática dos XMLs.
- Geração do relatório `Relatorio_Processamento.xlsx`.
- Geração do TXT `Chaves_Acesso_por_CT_e_Conta.txt`.

---

## Entrada de PDFs

A aplicação aceita duas formas de entrada:

### Pasta de PDFs

Pode ser selecionada uma pasta contendo PDFs.

A leitura é realizada de forma recursiva, incluindo todas as subpastas.

### ZIP de PDFs

Também é possível selecionar diretamente um arquivo `.zip`.

Nesse caso, a aplicação:

1. cria uma pasta temporária;
2. extrai o conteúdo do ZIP;
3. procura PDFs dentro de todas as pastas e subpastas;
4. processa os arquivos normalmente;
5. remove a pasta temporária ao finalizar.

O usuário não precisa descompactar o arquivo manualmente.

---

## Planilha de entrada

A primeira aba da planilha deve possuir, no mínimo, as seguintes colunas:

- `Conta`
- `Test name (initial)`

A planilha é utilizada apenas como base de consulta e não é alterada pela aplicação.

A conta extraída de cada PDF é comparada com a coluna `Conta`.

Quando existe correspondência, o valor de `Test name (initial)` determina o CT/cenário de destino.

Uma mesma conta pode estar relacionada a mais de um CT. Nesse caso, o PDF será considerado em todos os respectivos cenários.

---

## XMLs

O ZIP de XMLs é opcional.

### Execução sem XMLs

Se nenhum ZIP de XMLs for selecionado, a aplicação continua funcionando normalmente.

Ela:

- lê os PDFs;
- identifica contas;
- identifica chaves de acesso;
- separa os PDFs por cenário;
- identifica contas sem cenário;
- gera o relatório;
- gera o TXT de chaves.

Nesse caso, a quantidade de XMLs apresentada no relatório será `0`.

### Execução com XMLs

Caso seja informado um ZIP de XMLs, a aplicação também:

1. identifica as chaves existentes em cada PDF;
2. percorre os XMLs dentro do ZIP;
3. localiza os XMLs correspondentes;
4. copia os XMLs para a pasta da conta;
5. renomeia os XMLs automaticamente.

---

## Organização por cenário

Os arquivos são organizados utilizando o cenário definido na planilha.

Exemplo:

```text
Processamento_NFCom_2026-10-07
│
├── CT 001.01 - Emissão de NFCOM NORMAL...
│   │
│   ├── 000001156650437
│   │   ├── CT001.01.pdf
│   │   ├── NFCOM+RT27 - FTC - CT001.01 - NF110000008.xml
│   │   └── NFCOM+RT27 - FTC - CT001.01 - NF110000009.xml
│   │
│   └── 000073464460017
│       ├── CT001.01.pdf
│       └── NFCOM+RT27 - FTC - CT001.01 - NF110000010.xml
│
├── Contas sem Cenário
│   ├── 000517304790001.pdf
│   └── 000507491730111.pdf
│
├── Relatorio_Processamento.xlsx
│
└── Chaves_Acesso_por_CT_e_Conta.txt
```

---

## Contas sem cenário

Quando a aplicação encontra uma conta dentro de um PDF, mas essa conta não está cadastrada em nenhum CT da planilha, o documento é classificado como:

```text
Conta sem Cenário
```

Esses PDFs não são descartados.

Eles são enviados para:

```text
Contas sem Cenário
```

O PDF é renomeado utilizando apenas o número da conta.

Exemplo:

```text
Arquivo original:
260601500005_000007.PDF

Conta encontrada:
001274750830003

Arquivo final:
001274750830003.pdf
```

A regra utilizada é:

```text
1 conta = 1 PDF
```

---

## Relatório

O relatório principal gerado pela aplicação é:

```text
Relatorio_Processamento.xlsx
```

O arquivo contém as seguintes abas:

### Resumo

Apresenta as principais métricas da execução.

Exemplo:

```text
PDFs avaliados nesta execução: 197
DANFEs encontradas nesta execução: 335

Contas encontradas: 174
Contas não encontradas: 8
Contas sem Cenário: 3
Total de contas únicas na planilha: 182

Total de CTs: 78
Total de CTs Preenchidos: 68
Total de CTs Não Preenchidos: 10
```

### Contas Encontradas

Apresenta as contas localizadas durante o processamento e os respectivos cenários.

### Resumo por Cenário

Apresenta informações agrupadas por cenário, incluindo:

- cenário;
- quantidade de contas;
- conta;
- quantidade de XMLs encontrados.

### CTs Não Encontrados

Apresenta os CTs da planilha que não tiveram nenhuma conta encontrada durante o processamento.

Contém:

- CT;
- cenário;
- contas selecionadas.

### Contas sem CTs

Apresenta as contas encontradas nos PDFs que não possuem CT/cenário correspondente na planilha.

Contém somente:

| Conta | Qtd. DANFEs |
|---|---:|
| 001274750830003 | 1 |
| 000306393380004 | 2 |

---

## Diferença entre contas não encontradas e contas sem CTs

### Conta não encontrada

A conta existe na planilha e está associada a um CT, porém nenhum PDF correspondente foi localizado na execução.

### Conta sem CT

A conta foi encontrada dentro de um PDF, porém não existe na planilha de cenários.

Essa diferenciação evita que PDFs válidos sejam descartados.

---

## TXT de chaves de acesso

Além do relatório Excel, a aplicação gera:

```text
Chaves_Acesso_por_CT_e_Conta.txt
```

O arquivo organiza as chaves de acesso por CT e conta.

Exemplo:

```text
===============================================================================
CT 057.01
===============================================================================

CONTA: 000082724920052
NFCom13261040432544024321620573201141371077370654

CONTA: 000084327080001
NFCom13261040432544024321620573201141461022236866

CONTA: 000090625380001
NFCom13261040432544024321620573201141821031900532

===============================================================================
CT 057.02
===============================================================================

CONTA: 000109750410001
NFCom35261040432544000147620573202302671008268536

===============================================================================
```

Regras do TXT:

- agrupamento por CT;
- agrupamento por conta dentro do CT;
- cada chave possui o prefixo `NFCom`;
- uma conta pode possuir várias chaves;
- chaves repetidas não são duplicadas;
- se uma conta estiver associada a mais de um CT, ela aparecerá em todos eles;
- contas sem CT não são incluídas nesse arquivo;
- o TXT funciona mesmo quando nenhum ZIP de XMLs é informado.

---

## Associação dos XMLs

Quando o ZIP de XMLs é informado, cada DANFE-COM é relacionada aos respectivos XMLs através da chave de acesso NFCom.

A aplicação:

1. identifica as chaves de acesso presentes no PDF;
2. indexa os XMLs existentes dentro do ZIP;
3. procura os XMLs correspondentes;
4. copia os arquivos encontrados para a pasta da conta;
5. identifica o número da NFCom através do XML;
6. renomeia automaticamente o arquivo.

Padrão atual:

```text
NFCOM+RT27 - FTC - CT001.02 - NF110000008.xml
```

---

## Processamentos

Cada execução cria uma nova pasta utilizando a data atual.

Exemplo:

```text
Processamento_NFCom_2026-10-07
```

Caso já exista uma execução na mesma data:

```text
Processamento_NFCom_2026-10-07_2
Processamento_NFCom_2026-10-07_3
```

Cada execução é independente.

A aplicação não utiliza relatórios anteriores como histórico para determinar os resultados da execução atual.

---

## Tecnologias

- Java
- Swing
- Maven
- Apache PDFBox
- Apache POI
- XML DOM
- ZIP API do Java
- Inno Setup
- jpackage

---

## Execução para desenvolvimento

Compile o projeto:

```powershell
mvn clean compile
```

Execute:

```powershell
mvn exec:java "-Dexec.mainClass=com.ftcverificador.Main"
```

---

## Geração do instalador

O projeto possui scripts para geração do instalador Windows.

Execute:

```powershell
.\GERAR_INSTALADOR.bat
```

O processo:

1. compila o projeto;
2. gera o JAR executável;
3. cria a aplicação Windows com `jpackage`;
4. inclui o runtime Java;
5. gera o instalador através do Inno Setup.

O instalador pode ser distribuído para outros usuários sem necessidade de instalação manual do Java ou Maven.

---

## Arquivos principais

```text
src/main/java/com/ftcverificador/
│
├── Main.java
├── MainWindow.java
├── AppRunner.java
├── PdfInputService.java
├── PdfService.java
├── PdfExtractor.java
├── PdfContaDados.java
├── SpreadsheetService.java
├── XmlZipService.java
├── RunResult.java
└── ProcessingListener.java
```

---

## Objetivo

O objetivo do FTC Verificador PDF é reduzir o trabalho manual de organização e conferência dos documentos NFCom, centralizando em uma única execução:

- identificação das contas;
- identificação das chaves NFCom;
- associação com casos de teste;
- separação por CT/cenário;
- associação com XMLs;
- identificação de contas sem cenário;
- geração de relatórios;
- geração da relação de chaves de acesso.
```