package br.com.fiap.postech.carworkshop.workorder.adapter.presenter;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record DiagnosisResponse(
        Long id,
        StatusWO status,
        @JsonProperty("diagnostic_description") String diagnosticDescription,
        @JsonProperty("budget_value") BigDecimal budgetValue
) {
    public static DiagnosisResponse from(WorkOrder wo) {
        return new DiagnosisResponse(wo.getId(), wo.getStatus(),
                wo.getDiagnosticDescription(), wo.getBudgetValue());
    }
}
