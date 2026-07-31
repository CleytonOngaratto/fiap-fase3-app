package br.com.fiap.postech.carworkshop.autoservice.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AutoServicePanacheRepository implements PanacheRepository<AutoServiceJpaEntity> {

    public List<AutoServiceJpaEntity> findByIds(List<Long> ids) {
        return list("id in ?1", ids);
    }
}
