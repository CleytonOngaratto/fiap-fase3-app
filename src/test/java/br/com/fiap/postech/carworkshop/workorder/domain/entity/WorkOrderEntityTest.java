package br.com.fiap.postech.carworkshop.workorder.domain.entity;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.shared.domain.exception.InvalidOperationException;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderEntityTest {

    private Long customerId;
    private String customerEmail;
    private Long vehicleId;
    private String vehiclePlate;
    private AutoService service1;
    private AutoService service2;
    private List<AutoService> services;

    @BeforeEach
    void setUp() {
        customerId = 1L;
        customerEmail = "joao@test.com";
        vehicleId = 1L;
        vehiclePlate = "ABC-1234";

        service1 = AutoService.builder().id(1L).description("Troca de óleo").price(new BigDecimal("150.00")).build();
        service2 = AutoService.builder().id(2L).description("Alinhamento").price(new BigDecimal("100.00")).build();

        services = new ArrayList<>();
        services.add(service1);
        services.add(service2);
    }

    @Test
    void testCreate_Success() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        assertNotNull(workOrder);
        assertEquals(customerId, workOrder.getCustomerId());
        assertEquals(vehicleId, workOrder.getVehicleId());
        assertEquals(services, workOrder.getServices());
        assertEquals(StatusWO.RECEIVED, workOrder.getStatus());
        assertNotNull(workOrder.getCreationDate());
        assertNull(workOrder.getBudgetValue());
        assertNull(workOrder.getEndDate());
        assertNull(workOrder.getBudgetApprovalDate());
    }

    @Test
    void testCreate_InitializesCreationDate() {
        LocalDateTime before = LocalDateTime.now();
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(workOrder.getCreationDate());
        assertTrue(workOrder.getCreationDate().isAfter(before.minusSeconds(1)));
        assertTrue(workOrder.getCreationDate().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testGenerateBudget_Success() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();

        assertEquals(new BigDecimal("250.00"), workOrder.getBudgetValue());
        assertEquals(StatusWO.PENDING_APPROVAL, workOrder.getStatus());
    }

    @Test
    void testGenerateBudget_WithSingleService() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate,
                List.of(service1), List.of());
        workOrder.generateBudget();

        assertEquals(new BigDecimal("150.00"), workOrder.getBudgetValue());
        assertEquals(StatusWO.PENDING_APPROVAL, workOrder.getStatus());
    }

    @Test
    void testGenerateBudget_WithEmptyServices() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate,
                new ArrayList<>(), List.of());
        workOrder.generateBudget();

        assertEquals(BigDecimal.ZERO, workOrder.getBudgetValue());
        assertEquals(StatusWO.PENDING_APPROVAL, workOrder.getStatus());
    }

    @Test
    void testGenerateBudget_ChangesStatusToPendingApproval() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        assertEquals(StatusWO.RECEIVED, workOrder.getStatus());
        workOrder.generateBudget();
        assertEquals(StatusWO.PENDING_APPROVAL, workOrder.getStatus());
    }

    @Test
    void testApproveBudget_Success() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        LocalDateTime before = LocalDateTime.now();
        workOrder.approveBudget();
        LocalDateTime after = LocalDateTime.now();

        assertEquals(StatusWO.IN_PROGRESS, workOrder.getStatus());
        assertNotNull(workOrder.getBudgetApprovalDate());
        assertTrue(workOrder.getBudgetApprovalDate().isAfter(before.minusSeconds(1)));
        assertTrue(workOrder.getBudgetApprovalDate().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testApproveBudget_WhenStatusIsReceived_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class, () -> workOrder.approveBudget());
        assertEquals("Budget can only be approved when status is PENDING_APPROVAL.", exception.getMessage());
        assertEquals(StatusWO.RECEIVED, workOrder.getStatus());
    }

    @Test
    void testApproveBudget_WhenStatusIsInProgress_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.approveBudget();

        assertThrows(InvalidOperationException.class, () -> workOrder.approveBudget());
    }

    @Test
    void testApproveBudget_WhenStatusIsCanceled_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.rejectBudget();

        assertThrows(InvalidOperationException.class, () -> workOrder.approveBudget());
    }

    @Test
    void testRejectBudget_Success() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.rejectBudget();
        assertEquals(StatusWO.CANCELED, workOrder.getStatus());
    }

    @Test
    void testRejectBudget_WhenStatusIsReceived_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class, () -> workOrder.rejectBudget());
        assertEquals("Budget can only be rejected when status is PENDING_APPROVAL.", exception.getMessage());
        assertEquals(StatusWO.RECEIVED, workOrder.getStatus());
    }

    @Test
    void testCompleteService_Success() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.approveBudget();
        LocalDateTime before = LocalDateTime.now();
        workOrder.completeService();
        LocalDateTime after = LocalDateTime.now();

        assertEquals(StatusWO.COMPLETED, workOrder.getStatus());
        assertNotNull(workOrder.getEndDate());
        assertTrue(workOrder.getEndDate().isAfter(before.minusSeconds(1)));
        assertTrue(workOrder.getEndDate().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testCompleteService_WhenStatusIsReceived_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class, () -> workOrder.completeService());
        assertEquals("Work order can only be completed when status is IN_PROGRESS.", exception.getMessage());
    }

    @Test
    void testCompleteService_WhenStatusIsCanceled_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.rejectBudget();
        assertThrows(InvalidOperationException.class, () -> workOrder.completeService());
    }

    @Test
    void testDeliverVehicle_Success() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.approveBudget();
        workOrder.completeService();
        workOrder.deliverVehicle();
        assertEquals(StatusWO.DELIVERED, workOrder.getStatus());
    }

    @Test
    void testDeliverVehicle_WhenStatusIsReceived_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class, () -> workOrder.deliverVehicle());
        assertEquals("Vehicle can only be delivered when status is COMPLETED.", exception.getMessage());
    }

    @Test
    void testDeliverVehicle_WhenStatusIsInProgress_ThrowsException() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());
        workOrder.generateBudget();
        workOrder.approveBudget();
        assertThrows(InvalidOperationException.class, () -> workOrder.deliverVehicle());
    }

    @Test
    void testCompleteWorkflow_SuccessPath() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        assertEquals(StatusWO.RECEIVED, workOrder.getStatus());
        assertNotNull(workOrder.getCreationDate());

        workOrder.generateBudget();
        assertEquals(StatusWO.PENDING_APPROVAL, workOrder.getStatus());
        assertEquals(new BigDecimal("250.00"), workOrder.getBudgetValue());

        workOrder.approveBudget();
        assertEquals(StatusWO.IN_PROGRESS, workOrder.getStatus());
        assertNotNull(workOrder.getBudgetApprovalDate());

        workOrder.completeService();
        assertEquals(StatusWO.COMPLETED, workOrder.getStatus());
        assertNotNull(workOrder.getEndDate());

        workOrder.deliverVehicle();
        assertEquals(StatusWO.DELIVERED, workOrder.getStatus());
    }

    @Test
    void testCompleteWorkflow_RejectionPath() {
        WorkOrder workOrder = WorkOrder.create(customerId, customerEmail, vehicleId, vehiclePlate, services, List.of());

        assertEquals(StatusWO.RECEIVED, workOrder.getStatus());
        workOrder.generateBudget();
        assertEquals(StatusWO.PENDING_APPROVAL, workOrder.getStatus());
        workOrder.rejectBudget();
        assertEquals(StatusWO.CANCELED, workOrder.getStatus());

        assertThrows(InvalidOperationException.class, () -> workOrder.approveBudget());
        assertThrows(InvalidOperationException.class, () -> workOrder.completeService());
        assertThrows(InvalidOperationException.class, () -> workOrder.deliverVehicle());
    }
}
