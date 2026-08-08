package br.com.fiap.postech.carworkshop.workorder.adapter.presenter;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerWorkOrderMapperTest {

    private WorkOrder buildWorkOrder(StatusWO status) {
        return WorkOrder.builder()
                .id(1L).customerId(1L).vehicleId(1L).vehiclePlate("ABC-1234")
                .status(status).creationDate(LocalDateTime.now())
                .diagnosticDescription("Verificar sistema de freios")
                .budgetValue(new BigDecimal("250.00"))
                .services(List.of(
                        AutoService.builder().id(1L).description("Troca de Óleo").price(new BigDecimal("150.00")).build(),
                        AutoService.builder().id(2L).description("Alinhamento").price(new BigDecimal("100.00")).build()
                ))
                .parts(List.of())
                .build();
    }

    @Test
    void from_shouldMapWorkOrderToTrackingResponse() {
        WorkOrder workOrder = buildWorkOrder(StatusWO.RECEIVED);

        WorkOrderTrackingResponse response = WorkOrderTrackingResponse.from(workOrder);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(StatusWO.RECEIVED, response.status());
        assertEquals("ABC-1234", response.vehiclePlate());
        assertEquals("Verificar sistema de freios", response.diagnosticDescription());
        assertEquals(new BigDecimal("250.00"), response.budgetValue());
        assertEquals(2, response.services().size());
        assertFalse(response.actionsAvailable());
    }

    @Test
    void from_shouldSetActionsAvailableWhenPendingApproval() {
        WorkOrder workOrder = buildWorkOrder(StatusWO.PENDING_APPROVAL);

        WorkOrderTrackingResponse response = WorkOrderTrackingResponse.from(workOrder);

        assertEquals(StatusWO.PENDING_APPROVAL, response.status());
        assertTrue(response.actionsAvailable());
    }

    @Test
    void from_shouldHandleEmptyServicesList() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(3L).customerId(3L).vehicleId(3L).vehiclePlate("GHI-9012")
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of()).parts(List.of()).build();

        WorkOrderTrackingResponse response = WorkOrderTrackingResponse.from(workOrder);

        assertNotNull(response);
        assertEquals(3L, response.id());
        assertNotNull(response.services());
        assertTrue(response.services().isEmpty());
    }

    @Test
    void from_shouldMapAllStatuses() {
        for (StatusWO status : StatusWO.values()) {
            WorkOrder wo = buildWorkOrder(status);
            WorkOrderTrackingResponse response = WorkOrderTrackingResponse.from(wo);
            assertEquals(status, response.status());
            assertEquals(status == StatusWO.PENDING_APPROVAL, response.actionsAvailable());
        }
    }
}
