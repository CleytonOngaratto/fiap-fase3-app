package br.com.fiap.postech.carworkshop.workorder.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiagnosisRequest {

    @JsonProperty("diagnostic_description")
    private String diagnosticDescription;

    public String getDiagnosticDescription() { return diagnosticDescription; }
    public void setDiagnosticDescription(String diagnosticDescription) {
        this.diagnosticDescription = diagnosticDescription;
    }
}
