package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepositoryPort {
    WorkOrder save(WorkOrder workOrder);
    Optional<WorkOrder> findById(Long id);
    List<WorkOrder> findAll();
    List<WorkOrder> findAllActive();
    List<WorkOrder> findByStatus(StatusWO status);
    List<WorkOrder> findCompleted();
}
