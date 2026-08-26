package br.com.fiap.postech.carworkshop.workorder.usecase.interactor;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.InvalidOperationException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.StockException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.DiagnosisRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.WorkOrderRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.DiagnosisResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderDetailResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import br.com.fiap.postech.carworkshop.workorder.usecase.interactor.WorkOrderInteractor;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class WorkOrderInteractorTest {

    @InjectMocks
    WorkOrderInteractor interactor;

    @Mock
    WorkOrderRepositoryPort workOrderRepository;

    @Mock
    CustomerDataPort customerDataPort;

    @Mock
    VehicleDataPort vehicleDataPort;

    @Mock
    AutoServiceDataPort autoServiceDataPort;

    @Mock
    InventoryDataPort inventoryDataPort;

    @Mock
    WorkOrderNotificationPort notificationPort;

    @Mock
    WorkOrderMetricsPort metricsPort;

    private AutoService autoServiceDomain;
    private PartsAndSupply partsDomain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        autoServiceDomain = AutoService.builder().id(1L).description("Troca de óleo").price(new BigDecimal("100.00")).build();
        partsDomain = PartsAndSupply.builder().id(1L).description("Filtro de óleo").price(new BigDecimal("50.00")).quantity(10).build();
    }

    private WorkOrderRequest validRequest() {
        WorkOrderRequest req = new WorkOrderRequest();
        req.setCustomerId(1L);
        req.setVehicleId(1L);
        req.setServiceIds(List.of(1L));
        return req;
    }

    private WorkOrder workOrderInStatus(StatusWO status) {
        return WorkOrder.builder()
                .id(1L).customerId(1L).customerEmail("joao@email.com")
                .vehicleId(1L).vehiclePlate("ABC-1234").status(status)
                .creationDate(LocalDateTime.now()).services(List.of(autoServiceDomain))
                .parts(List.of()).build();
    }

    @Test
    void testCreate_Success() {
        when(customerDataPort.findById(1L)).thenReturn(Optional.of(new CustomerDataPort.CustomerInfo(1L, "joao@email.com")));
        when(vehicleDataPort.findById(1L)).thenReturn(Optional.of(new VehicleDataPort.VehicleInfo(1L, "ABC-1234")));
        when(autoServiceDataPort.findByIds(anyList())).thenReturn(List.of(autoServiceDomain));
        WorkOrder saved = workOrderInStatus(StatusWO.RECEIVED);
        when(workOrderRepository.save(any())).thenReturn(saved);

        WorkOrderResponse result = interactor.create(validRequest());

        assertNotNull(result);
        assertEquals(StatusWO.RECEIVED, result.status());
        verify(workOrderRepository).save(any());
    }

    @Test
    void testCreate_NoServices_ThrowsValidation() {
        WorkOrderRequest req = new WorkOrderRequest();
        req.setCustomerId(1L);
        req.setVehicleId(1L);
        req.setServiceIds(List.of());

        assertThrows(ValidationException.class, () -> interactor.create(req));
        verifyNoInteractions(workOrderRepository);
    }

    @Test
    void testCreate_CustomerNotFound() {
        when(customerDataPort.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.create(validRequest()));
        verifyNoInteractions(workOrderRepository);
    }

    @Test
    void testCreate_VehicleNotFound() {
        when(customerDataPort.findById(1L)).thenReturn(Optional.of(new CustomerDataPort.CustomerInfo(1L, "email@test.com")));
        when(vehicleDataPort.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.create(validRequest()));
    }

    @Test
    void testCreate_ServicesNotFound() {
        when(customerDataPort.findById(1L)).thenReturn(Optional.of(new CustomerDataPort.CustomerInfo(1L, "email@test.com")));
        when(vehicleDataPort.findById(1L)).thenReturn(Optional.of(new VehicleDataPort.VehicleInfo(1L, "ABC-1234")));
        when(autoServiceDataPort.findByIds(anyList())).thenReturn(List.of());
        assertThrows(EntityNotFoundException.class, () -> interactor.create(validRequest()));
        verifyNoInteractions(workOrderRepository);
    }

    @Test
    void testCreate_NullCustomerId_ThrowsValidation() {
        WorkOrderRequest req = new WorkOrderRequest();
        req.setVehicleId(1L);
        req.setServiceIds(List.of(1L));
        assertThrows(ValidationException.class, () -> interactor.create(req));
        verifyNoInteractions(workOrderRepository);
    }

    @Test
    void testCompleteDiagnosis_Success() {
        WorkOrder wo = workOrderInStatus(StatusWO.UNDER_DIAGNOSIS);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        DiagnosisRequest diagReq = new DiagnosisRequest();
        diagReq.setDiagnosticDescription("Engine problem found");
        DiagnosisResponse result = interactor.completeDiagnosis(1L, diagReq);

        assertNotNull(result);
        assertEquals(StatusWO.PENDING_APPROVAL, result.status());
    }

    @Test
    void testCompleteDiagnosis_WrongStatus_ThrowsValidation() {
        WorkOrder wo = workOrderInStatus(StatusWO.RECEIVED);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

        DiagnosisRequest diagReq = new DiagnosisRequest();
        diagReq.setDiagnosticDescription("desc");
        assertThrows(ValidationException.class, () -> interactor.completeDiagnosis(1L, diagReq));
    }

    @Test
    void testCompleteDiagnosis_BlankDescription_ThrowsValidation() {
        DiagnosisRequest diagReq = new DiagnosisRequest();
        diagReq.setDiagnosticDescription("");
        assertThrows(ValidationException.class, () -> interactor.completeDiagnosis(1L, diagReq));
        verifyNoInteractions(workOrderRepository);
    }

    @Test
    void testCompleteWorkOrder_Success() {
        WorkOrder wo = workOrderInStatus(StatusWO.IN_PROGRESS);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);
        doNothing().when(notificationPort).notifyCompleted(any());

        WorkOrderDetailResponse result = interactor.completeWorkOrder(1L);

        assertNotNull(result);
        assertEquals(StatusWO.COMPLETED, result.status());
        verify(notificationPort).notifyCompleted(any());
    }

    @Test
    void testCompleteWorkOrder_NotFound() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.completeWorkOrder(999L));
        verify(notificationPort, never()).notifyCompleted(any());
    }

    @Test
    void testCompleteWorkOrder_WrongStatus_ThrowsInvalidOperation() {
        WorkOrder wo = workOrderInStatus(StatusWO.RECEIVED);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        assertThrows(InvalidOperationException.class, () -> interactor.completeWorkOrder(1L));
    }

    @Test
    void testDeliverWorkOrder_Success() {
        WorkOrder wo = workOrderInStatus(StatusWO.COMPLETED);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        WorkOrderDetailResponse result = interactor.deliverWorkOrder(1L);

        assertNotNull(result);
        assertEquals(StatusWO.DELIVERED, result.status());
    }

    @Test
    void testDeliverWorkOrder_NotFound() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.deliverWorkOrder(999L));
    }

    @Test
    void testApproveWorkOrder_Success_NoParts() {
        WorkOrder wo = workOrderInStatus(StatusWO.PENDING_APPROVAL);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        WorkOrderTrackingResponse result = interactor.approveWorkOrder(1L);

        assertNotNull(result);
        assertEquals(StatusWO.IN_PROGRESS, result.status());
    }

    @Test
    void testApproveWorkOrder_WithParts_ConsumesStock() {
        WorkOrder wo = WorkOrder.builder()
                .id(1L).customerId(1L).customerEmail("joao@email.com")
                .vehicleId(1L).vehiclePlate("ABC-1234").status(StatusWO.PENDING_APPROVAL)
                .creationDate(LocalDateTime.now()).services(List.of(autoServiceDomain))
                .parts(List.of(partsDomain)).build();

        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);
        doNothing().when(inventoryDataPort).consumeStock(1L);

        interactor.approveWorkOrder(1L);

        verify(inventoryDataPort).consumeStock(1L);
    }

    @Test
    void testApproveWorkOrder_InsufficientStock_ThrowsStockException() {
        WorkOrder wo = WorkOrder.builder()
                .id(1L).customerId(1L).customerEmail("joao@email.com")
                .vehicleId(1L).vehiclePlate("ABC-1234").status(StatusWO.PENDING_APPROVAL)
                .creationDate(LocalDateTime.now()).services(List.of(autoServiceDomain))
                .parts(List.of(partsDomain)).build();

        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        doThrow(new StockException("Insufficient stock.")).when(inventoryDataPort).consumeStock(1L);

        assertThrows(StockException.class, () -> interactor.approveWorkOrder(1L));
    }

    @Test
    void testRejectWorkOrder_Success() {
        WorkOrder wo = workOrderInStatus(StatusWO.PENDING_APPROVAL);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        WorkOrderTrackingResponse result = interactor.rejectWorkOrder(1L);

        assertNotNull(result);
        assertEquals(StatusWO.CANCELED, result.status());
    }

    @Test
    void testRejectWorkOrder_NotFound() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.rejectWorkOrder(999L));
    }

    @Test
    void testFindById_NotFound() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.findById(999L));
    }

    @Test
    void testGetAverageCompletionTime_NoCompleted_ReturnsZero() {
        when(workOrderRepository.findCompleted()).thenReturn(List.of());
        assertEquals(0.0, interactor.getAverageCompletionTimeInHours());
    }

    @Test
    void testGetAverageCompletionTime_WithCompleted() {
        WorkOrder wo = WorkOrder.builder()
                .id(1L).status(StatusWO.COMPLETED)
                .creationDate(LocalDateTime.now().minusHours(4))
                .endDate(LocalDateTime.now()).build();
        when(workOrderRepository.findCompleted()).thenReturn(List.of(wo));

        Double avg = interactor.getAverageCompletionTimeInHours();
        assertNotNull(avg);
        assertTrue(avg > 0);
    }

    @Test
    void testApproveWorkOrder_RecordsStatusMetric() {
        WorkOrder wo = workOrderInStatus(StatusWO.PENDING_APPROVAL);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        interactor.approveWorkOrder(1L);

        verify(metricsPort).recordStatusChange(StatusWO.IN_PROGRESS);
    }

    @Test
    void testRejectWorkOrder_RecordsStatusMetric() {
        WorkOrder wo = workOrderInStatus(StatusWO.PENDING_APPROVAL);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        interactor.rejectWorkOrder(1L);

        verify(metricsPort).recordStatusChange(StatusWO.CANCELED);
    }

    @Test
    void testCompleteWorkOrder_RecordsStatusAndCompletionTime() {
        WorkOrder wo = workOrderInStatus(StatusWO.IN_PROGRESS);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);
        doNothing().when(notificationPort).notifyCompleted(any());

        interactor.completeWorkOrder(1L);

        verify(metricsPort).recordStatusChange(StatusWO.COMPLETED);
        verify(metricsPort).recordCompletion(any(Duration.class));
    }

    @Test
    void testDeliverWorkOrder_RecordsStatusMetric() {
        WorkOrder wo = workOrderInStatus(StatusWO.COMPLETED);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any())).thenReturn(wo);

        interactor.deliverWorkOrder(1L);

        verify(metricsPort).recordStatusChange(StatusWO.DELIVERED);
    }

    @Test
    void testCompleteWorkOrder_NotFound_RecordsNoMetric() {
        when(workOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> interactor.completeWorkOrder(999L));

        verify(metricsPort, never()).recordStatusChange(any());
        verify(metricsPort, never()).recordCompletion(any());
    }
}
