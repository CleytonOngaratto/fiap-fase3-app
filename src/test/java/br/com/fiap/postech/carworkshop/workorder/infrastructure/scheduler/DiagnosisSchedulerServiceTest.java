package br.com.fiap.postech.carworkshop.workorder.infrastructure.scheduler;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderJpaEntity;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderPanacheRepository;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.scheduler.DiagnosisSchedulerService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@QuarkusTest
class DiagnosisSchedulerServiceTest {

    @Inject
    DiagnosisSchedulerService diagnosisSchedulerService;

    @InjectMock
    WorkOrderPanacheRepository workOrderRepository;

    private WorkOrderJpaEntity workOrder1;
    private WorkOrderJpaEntity workOrder2;

    @BeforeEach
    void setUp() {
        workOrder1 = mock(WorkOrderJpaEntity.class);
        workOrder1.id = 1L;
        when(workOrder1.getStatus()).thenReturn(StatusWO.RECEIVED);

        workOrder2 = mock(WorkOrderJpaEntity.class);
        workOrder2.id = 2L;
        when(workOrder2.getStatus()).thenReturn(StatusWO.RECEIVED);
    }

    @Test
    void testScheduleDiagnosisForNewOrders_Success() {
        when(workOrderRepository.findByStatus(StatusWO.RECEIVED)).thenReturn(List.of(workOrder1, workOrder2));

        diagnosisSchedulerService.scheduleDiagnosisForNewOrders();

        verify(workOrderRepository, times(1)).findByStatus(StatusWO.RECEIVED);
        verify(workOrder1, times(1)).setStatus(StatusWO.UNDER_DIAGNOSIS);
        verify(workOrder2, times(1)).setStatus(StatusWO.UNDER_DIAGNOSIS);
    }

    @Test
    void testScheduleDiagnosisForNewOrders_NoReceivedOrders() {
        when(workOrderRepository.findByStatus(StatusWO.RECEIVED)).thenReturn(Collections.emptyList());

        diagnosisSchedulerService.scheduleDiagnosisForNewOrders();

        verify(workOrderRepository, times(1)).findByStatus(StatusWO.RECEIVED);
        verify(workOrder1, never()).setStatus(any());
    }

    @Test
    void testScheduleDiagnosisForNewOrders_SingleOrder() {
        when(workOrderRepository.findByStatus(StatusWO.RECEIVED)).thenReturn(List.of(workOrder1));

        diagnosisSchedulerService.scheduleDiagnosisForNewOrders();

        verify(workOrder1, times(1)).setStatus(StatusWO.UNDER_DIAGNOSIS);
    }
}
