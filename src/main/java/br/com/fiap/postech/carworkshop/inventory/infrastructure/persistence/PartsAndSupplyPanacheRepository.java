package br.com.fiap.postech.carworkshop.inventory.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PartsAndSupplyPanacheRepository implements PanacheRepository<PartsAndSupplyJpaEntity> {

    public List<PartsAndSupplyJpaEntity> findByIds(List<Long> ids) {
        return list("id in ?1", ids);
    }
}
