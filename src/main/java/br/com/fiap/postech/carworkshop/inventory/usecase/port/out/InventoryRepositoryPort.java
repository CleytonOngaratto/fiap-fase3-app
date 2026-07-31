package br.com.fiap.postech.carworkshop.inventory.usecase.port.out;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;

import java.util.List;
import java.util.Optional;

public interface InventoryRepositoryPort {
    List<PartsAndSupply> findAll();
    Optional<PartsAndSupply> findById(Long id);
    List<PartsAndSupply> findByIds(List<Long> ids);
    PartsAndSupply save(PartsAndSupply part);
    boolean deleteById(Long id);
}
