package br.com.fiap.postech.carworkshop.inventory.infrastructure.mapper;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.inventory.infrastructure.persistence.PartsAndSupplyJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface PartsAndSupplyJpaMapper {
    PartsAndSupply toDomain(PartsAndSupplyJpaEntity entity);
    PartsAndSupplyJpaEntity toJpaEntity(PartsAndSupply domain);
}
