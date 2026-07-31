package br.com.fiap.postech.carworkshop.autoservice.infrastructure.mapper;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.autoservice.infrastructure.persistence.AutoServiceJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface AutoServiceJpaMapper {
    AutoService toDomain(AutoServiceJpaEntity entity);
    AutoServiceJpaEntity toJpaEntity(AutoService domain);
}
