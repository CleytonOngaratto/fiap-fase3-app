package br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class WorkOrderPanacheRepository implements PanacheRepository<WorkOrderJpaEntity> {

    public List<WorkOrderJpaEntity> findByStatus(StatusWO status) {
        return list("SELECT wo FROM WorkOrderJpaEntity wo LEFT JOIN FETCH wo.services WHERE wo.status = ?1 AND wo.deleted = false", status);
    }

    public List<WorkOrderJpaEntity> findCompleted() {
        return list("status in ?1 and endDate is not null and deleted = false",
                List.of(StatusWO.COMPLETED, StatusWO.DELIVERED));
    }

    public List<WorkOrderJpaEntity> findAllActive() {
        return list("deleted = false ORDER BY creationDate ASC");
    }
}
