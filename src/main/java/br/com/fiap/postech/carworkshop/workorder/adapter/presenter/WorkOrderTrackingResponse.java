package br.com.fiap.postech.carworkshop.workorder.adapter.presenter;

import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.inventory.adapter.presenter.PartsAndSupplyResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record WorkOrderTrackingResponse(
        Long id,
        StatusWO status,
        @JsonProperty("vehicle_plate") String vehiclePlate,
        @JsonProperty("diagnostic_description") String diagnosticDescription,
        @JsonProperty("budget_value") BigDecimal budgetValue,
        List<AutoServiceResponse> services,
        List<PartsAndSupplyResponse> parts,
        @JsonProperty("actions_available") boolean actionsAvailable
) {
    public static WorkOrderTrackingResponse from(WorkOrder wo) {
        return new WorkOrderTrackingResponse(
                wo.getId(),
                wo.getStatus(),
                wo.getVehiclePlate(),
                wo.getDiagnosticDescription(),
                wo.getBudgetValue(),
                wo.getServices() != null
                        ? wo.getServices().stream().map(AutoServiceResponse::from).toList()
                        : List.of(),
                wo.getParts() != null
                        ? wo.getParts().stream().map(PartsAndSupplyResponse::from).toList()
                        : List.of(),
                wo.getStatus() == StatusWO.PENDING_APPROVAL
        );
    }
}
