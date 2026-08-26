$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

function Require-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Name,
        [Parameter(Mandatory = $true)]
        [string] $InstallMessage
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name não foi encontrado. $InstallMessage"
    }
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor DarkRed
Write-Host "  FTC Verificador PDF - Gerar instalador" -ForegroundColor Red
Write-Host "==============================================" -ForegroundColor DarkRed
Write-Host ""

Require-Command `
    -Name "mvn" `
    -InstallMessage "Instale o Maven e adicione a pasta bin ao PATH."

Require-Command `
    -Name "jpackage" `
    -InstallMessage "Use um JDK 17 ou superior. O JDK 21 é recomendado para gerar o aplicativo."

$InnoCandidates = @(
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "${env:ProgramFiles}\Inno Setup 6\ISCC.exe",
    "${env:LOCALAPPDATA}\Programs\Inno Setup 6\ISCC.exe"
)

$InnoCompiler = $InnoCandidates |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1

if (-not $InnoCompiler) {
    throw "Inno Setup 6 não foi encontrado. Instale o Inno Setup antes de gerar o instalador."
}

Write-Host "[1/4] Compilando o projeto..." -ForegroundColor Cyan
& mvn clean package
if ($LASTEXITCODE -ne 0) {
    throw "A compilação Maven falhou."
}

$FatJar = Join-Path $ProjectRoot "target\ftc-verificador-pdf-1.0.0-all.jar"

if (-not (Test-Path $FatJar)) {
    throw "O arquivo executável JAR não foi gerado: $FatJar"
}

$JPackageOutput = Join-Path $ProjectRoot "target\installer-build"
if (Test-Path $JPackageOutput) {
    Remove-Item $JPackageOutput -Recurse -Force
}
New-Item -ItemType Directory -Path $JPackageOutput | Out-Null

Write-Host "[2/4] Criando o aplicativo Windows..." -ForegroundColor Cyan
& jpackage `
    --type app-image `
    --input (Join-Path $ProjectRoot "target") `
    --dest $JPackageOutput `
    --name "FTC Verificador PDF" `
    --main-jar "ftc-verificador-pdf-1.0.0-all.jar" `
    --main-class "com.ftcverificador.Main" `
    --icon (Join-Path $ProjectRoot "installer\app-icon.ico") `
    --vendor "FTC" `
    --app-version "1.0.0" `
    --description "Cruza contas em PDFs e organiza os arquivos por cenário de teste"

if ($LASTEXITCODE -ne 0) {
    throw "O jpackage não conseguiu criar o aplicativo."
}

$DistDir = Join-Path $ProjectRoot "dist"
if (Test-Path $DistDir) {
    Remove-Item $DistDir -Recurse -Force
}
New-Item -ItemType Directory -Path $DistDir | Out-Null

Write-Host "[3/4] Gerando o instalador..." -ForegroundColor Cyan
& $InnoCompiler (Join-Path $ProjectRoot "installer\FTC-Verificador-PDF.iss")
if ($LASTEXITCODE -ne 0) {
    throw "O Inno Setup não conseguiu gerar o instalador."
}

$Installer = Join-Path $DistDir "Instalar_FTC_Verificador_PDF_1.0.0.exe"

if (-not (Test-Path $Installer)) {
    throw "O instalador não foi encontrado em: $Installer"
}

Write-Host "[4/4] Finalizado." -ForegroundColor Green
Write-Host ""
Write-Host "Instalador criado em:" -ForegroundColor Green
Write-Host $Installer -ForegroundColor White
Write-Host ""
Write-Host "O computador do usuário não precisará ter Java instalado." -ForegroundColor Yellow
Write-Host ""

Start-Process explorer.exe -ArgumentList "/select,`"$Installer`""
