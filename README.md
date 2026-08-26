# FTC Verificador PDF

Aplicação desktop desenvolvida em Java para auxiliar na análise e organização de arquivos relacionados à NFCom.

A automação cruza informações encontradas em arquivos DANFE-COM com uma planilha de casos de teste e relaciona os respectivos XMLs utilizando a chave de acesso da NFCom.

## Funcionalidades

* Leitura de arquivos PDF e subpastas.
* Identificação das contas presentes nos DANFE-COM.
* Identificação das chaves de acesso NFCom.
* Contagem de PDFs analisados.
* Contagem de DANFEs encontradas.
* Cruzamento das contas com uma planilha Excel.
* Identificação do cenário de teste por `Test name (initial)`.
* Leitura de XMLs armazenados em arquivo ZIP.
* Associação entre DANFE-COM e XML pela chave de acesso.
* Organização automática dos arquivos por cenário de teste.
* Criação de uma subpasta para cada PDF.
* Renomeação automática dos XMLs.
* Geração do relatório `Contas_Encontradas.xlsx`.

## Estrutura de saída

Exemplo:

```text
Resultado
│
├── Cenário de Teste
│   │
│   ├── PDF_001
│   │   ├── PDF_001.pdf
│   │   ├── NFCOM+RT27 - FAT - FTC - CT001.02 - XML - NF110000008.xml
│   │   └── NFCOM+RT27 - FAT - FTC - CT001.02 - XML - NF110000009.xml
│   │
│   └── PDF_002
│       ├── PDF_002.pdf
│       └── NFCOM+RT27 - FAT - FTC - CT001.02 - XML - NF110000010.xml
│
└── Contas_Encontradas.xlsx
```

## Planilha de entrada

A primeira aba da planilha deve possuir as seguintes colunas:

* `Conta`
* `Test name (initial)`

A planilha é utilizada somente para consulta e não é alterada pela aplicação.

## Associação dos XMLs

Cada DANFE-COM é relacionada ao seu XML através da chave de acesso NFCom.

A aplicação:

1. identifica as chaves existentes em cada PDF;
2. percorre os XMLs existentes dentro do arquivo ZIP;
3. encontra o XML correspondente;
4. copia o XML para a pasta do respectivo PDF;
5. renomeia o arquivo conforme o cenário e o número da NFCom.

Padrão:

```text
NFCOM+RT27 - FAT - FTC - CT001.02 - XML - NF110000008.xml
```

## Relatório

O arquivo `Contas_Encontradas.xlsx` apresenta um resumo da execução, incluindo:

* PDFs avaliados;
* DANFEs encontradas;
* contas encontradas.

Também contém o detalhamento das contas e dos respectivos casos de teste e PDFs.

## Tecnologias

* Java
* Swing
* Maven
* Apache PDFBox
* Apache POI
* XML DOM
* Inno Setup
* jpackage

## Execução para desenvolvimento

Compile o projeto:

```powershell
mvn clean compile
```

Execute:

```powershell
mvn exec:java "-Dexec.mainClass=com.ftcverificador.Main"
```

## Instalador

O projeto possui scripts para geração do instalador Windows.

```powershell
.\GERAR_INSTALADOR.bat
```

O instalador gerado pode ser distribuído para os usuários sem necessidade de configuração manual do ambiente Java.
