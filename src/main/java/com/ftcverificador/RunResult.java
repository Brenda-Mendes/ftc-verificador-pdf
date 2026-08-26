package com.ftcverificador;

public class RunResult {

    private final int arquivosLidos;
    private final int contasEncontradas;
    private final int danfesEncontradas;

    public RunResult(
            int arquivosLidos,
            int contasEncontradas,
            int danfesEncontradas
    ) {

        this.arquivosLidos =
                arquivosLidos;

        this.contasEncontradas =
                contasEncontradas;

        this.danfesEncontradas =
                danfesEncontradas;
    }

    public int getArquivosLidos() {
        return arquivosLidos;
    }

    public int getContasEncontradas() {
        return contasEncontradas;
    }

    public int getDanfesEncontradas() {
        return danfesEncontradas;
    }
}