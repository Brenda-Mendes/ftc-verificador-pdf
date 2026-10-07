package com.ftcverificador;

import java.nio.file.Path;

public class RunResult {

    private final int arquivosLidos;
    private final int contasEncontradas;
    private final int contasNaoEncontradas;
    private final int contasSemCenario;
    private final int totalContasPlanilha;
    private final int danfesEncontradas;
    private final Path outputDir;
    private final Path reportPath;

    public RunResult(
            int arquivosLidos,
            int contasEncontradas,
            int contasNaoEncontradas,
            int contasSemCenario,
            int totalContasPlanilha,
            int danfesEncontradas,
            Path outputDir,
            Path reportPath
    ) {
        this.arquivosLidos = arquivosLidos;
        this.contasEncontradas = contasEncontradas;
        this.contasNaoEncontradas = contasNaoEncontradas;
        this.contasSemCenario = contasSemCenario;
        this.totalContasPlanilha = totalContasPlanilha;
        this.danfesEncontradas = danfesEncontradas;
        this.outputDir = outputDir;
        this.reportPath = reportPath;
    }

    public int getArquivosLidos() {
        return arquivosLidos;
    }

    public int getContasEncontradas() {
        return contasEncontradas;
    }

    public int getContasNaoEncontradas() {
        return contasNaoEncontradas;
    }

    public int getContasSemCenario() {
        return contasSemCenario;
    }

    public int getTotalContasPlanilha() {
        return totalContasPlanilha;
    }

    public int getDanfesEncontradas() {
        return danfesEncontradas;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public Path getReportPath() {
        return reportPath;
    }
}
