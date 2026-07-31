package br.com.fiap.postech.carworkshop.workorder.adapter.presenter;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderCreateResponseMapperTest {

    @Test
    void from_shouldMapWorkOrderToResponse() {
        AutoService service1 = AutoService.builder().id(1L).description("Troca de Óleo").price(new BigDecimal("150.00")).build();
        AutoService service2 = AutoService.builder().id(2L).description("Alinhamento").price(new BigDecimal("100.00")).build();

        WorkOrder workOrder = WorkOrder.builder()
                .id(10L).customerId(1L).vehicleId(2L)
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of(service1, service2)).parts(List.of())
                .build();

        WorkOrderResponse response = WorkOrderResponse.from(workOrder);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.customerId());
        assertEquals(2L, response.vehicleId());
        assertEquals(StatusWO.RECEIVED, response.status());
        assertNotNull(response.creationDate());
        assertNull(response.endDate());
        assertEquals(2, response.services().size());
    }

    @Test
    void from_shouldMapCustomerIdCorrectly() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(100L).customerId(99L).vehicleId(88L)
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of()).parts(List.of())
                .build();

        WorkOrderResponse response = WorkOrderResponse.from(workOrder);

        assertEquals(99L, response.customerId());
        assertEquals(88L, response.vehicleId());
        assertEquals(100L, response.id());
    }

    @Test
    void from_shouldHandleEmptyServices() {
        WorkOrder workOrder = WorkOrder.builder()
                .id(20L).customerId(5L).vehicleId(6L)
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(List.of()).parts(List.of())
                .build();

        WorkOrderResponse response = WorkOrderResponse.from(workOrder);

        assertNotNull(response);
        assertNotNull(response.services());
        assertTrue(response.services().isEmpty());
    }

    @Test
    void from_shouldMapMultipleServices() {
        List<AutoService> services = List.of(
                AutoService.builder().id(1L).description("Troca de Óleo").price(new BigDecimal("150.00")).build(),
                AutoService.builder().id(2L).description("Alinhamento").price(new BigDecimal("100.00")).build(),
                AutoService.builder().id(3L).description("Balanceamento").price(new BigDecimal("80.00")).build()
        );

        WorkOrder workOrder = WorkOrder.builder()
                .id(40L).customerId(10L).vehicleId(11L)
                .status(StatusWO.RECEIVED).creationDate(LocalDateTime.now())
                .services(services).parts(List.of())
                .build();

        WorkOrderResponse response = WorkOrderResponse.from(workOrder);

        assertEquals(3, response.services().size());
        assertEquals("Troca de Óleo", response.services().get(0).description());
        assertEquals("Alinhamento", response.services().get(1).description());
    }
}
