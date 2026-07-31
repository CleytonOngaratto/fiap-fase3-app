package br.com.fiap.postech.carworkshop.auth.infrastructure.mapper;

import br.com.fiap.postech.carworkshop.auth.domain.entity.User;
import br.com.fiap.postech.carworkshop.auth.infrastructure.persistence.UserJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface UserJpaMapper {
    User toDomain(UserJpaEntity entity);
    UserJpaEntity toJpaEntity(User domain);
}
