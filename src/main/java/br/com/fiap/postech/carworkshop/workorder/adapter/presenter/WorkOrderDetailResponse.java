package br.com.fiap.postech.carworkshop.workorder.adapter.presenter;

import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.inventory.adapter.presenter.PartsAndSupplyResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderDetailResponse(
        Long id,
        @JsonProperty("customer_id") Long customerId,
        @JsonProperty("vehicle_id") Long vehicleId,
        StatusWO status,
        @JsonProperty("creation_date") LocalDateTime creationDate,
        @JsonProperty("end_date") LocalDateTime endDate,
        @JsonProperty("diagnostic_description") String diagnosticDescription,
        @JsonProperty("budget_value") BigDecimal budgetValue,
        List<AutoServiceResponse> services,
        List<PartsAndSupplyResponse> parts
) {
    public static WorkOrderDetailResponse from(WorkOrder wo) {
        return new WorkOrderDetailResponse(
                wo.getId(),
                wo.getCustomerId(),
                wo.getVehicleId(),
                wo.getStatus(),
                wo.getCreationDate(),
                wo.getEndDate(),
                wo.getDiagnosticDescription(),
                wo.getBudgetValue(),
                wo.getServices() != null
                        ? wo.getServices().stream().map(AutoServiceResponse::from).toList()
                        : List.of(),
                wo.getParts() != null
                        ? wo.getParts().stream().map(PartsAndSupplyResponse::from).toList()
                        : List.of()
        );
    }
}
