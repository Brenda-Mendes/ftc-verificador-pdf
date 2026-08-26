package com.ftcverificador;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class PdfContaDados {

    private final Set<String> contas;
    private final Set<String> chavesAcesso;

    private final String nfCom;
    private final String tipo;
    private final String finalidade;

    public PdfContaDados(
            Set<String> contas,
            Set<String> chavesAcesso,
            String nfCom,
            String tipo,
            String finalidade
    ) {

        this.contas =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(contas)
                );

        this.chavesAcesso =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(chavesAcesso)
                );

        this.nfCom = emptyIfNull(nfCom);
        this.tipo = emptyIfNull(tipo);
        this.finalidade = emptyIfNull(finalidade);
    }

    public Set<String> getContas() {
        return contas;
    }

    public Set<String> getChavesAcesso() {
        return chavesAcesso;
    }

    public String getNfCom() {
        return nfCom;
    }

    public String getTipo() {
        return tipo;
    }

    public String getFinalidade() {
        return finalidade;
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}