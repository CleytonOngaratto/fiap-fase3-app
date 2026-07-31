package br.com.fiap.postech.carworkshop.workorder.infrastructure.mapper;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderJpaEntity;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderPartSnapshot;
import br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence.WorkOrderServiceSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta")
public interface WorkOrderJpaMapper {

    WorkOrder toDomain(WorkOrderJpaEntity entity);

    WorkOrderJpaEntity toJpaEntity(WorkOrder domain);

    @Mapping(target = "id", source = "autoServiceId")
    AutoService serviceToDomain(WorkOrderServiceSnapshot snapshot);

    @Mapping(target = "autoServiceId", source = "id")
    WorkOrderServiceSnapshot toServiceSnapshot(AutoService service);

    @Mapping(target = "id", source = "partsAndSupplyId")
    PartsAndSupply partToDomain(WorkOrderPartSnapshot snapshot);

    @Mapping(target = "partsAndSupplyId", source = "id")
    WorkOrderPartSnapshot toPartSnapshot(PartsAndSupply part);
}
