package br.com.fiap.postech.carworkshop.customer.infrastructure.mapper;

import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;
import br.com.fiap.postech.carworkshop.customer.infrastructure.persistence.CustomerJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface CustomerJpaMapper {

    Customer toDomain(CustomerJpaEntity entity);

    CustomerJpaEntity toJpaEntity(Customer domain);
}
