package br.com.fiap.postech.carworkshop.workorder.infrastructure.scheduler;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderJpaEntity;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderPanacheRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
public class DiagnosisSchedulerService {

    @Inject
    WorkOrderPanacheRepository workOrderRepository;

    @Scheduled(every = "10s", identity = "diagnosis-scheduler")
    @Transactional
    public void scheduleDiagnosisForNewOrders() {
        // No per-tick logging: this runs every 10s and would flood the logs. Only the actual
        // status change below is logged, so the output stays meaningful.
        List<WorkOrderJpaEntity> receivedOrders = workOrderRepository.findByStatus(StatusWO.RECEIVED);
        if (receivedOrders.isEmpty()) {
            return;
        }
        for (WorkOrderJpaEntity workOrder : receivedOrders) {
            workOrder.setStatus(StatusWO.UNDER_DIAGNOSIS);
            log.info("WO ID {} status changed to {}", workOrder.id, StatusWO.UNDER_DIAGNOSIS);
        }
    }
}
