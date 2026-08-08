package br.com.fiap.postech.carworkshop.workorder.adapter.gateway;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.out.InventoryRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.InventoryDataPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class InventoryDataGateway implements InventoryDataPort {

    @Inject
    InventoryRepositoryPort inventoryRepository;

    @Override
    public List<PartsAndSupply> findByIds(List<Long> ids) {
        return inventoryRepository.findByIds(ids);
    }

    @Override
    @Transactional
    public void consumeStock(Long partId) {
        PartsAndSupply part = inventoryRepository.findById(partId)
                .orElseThrow(() -> new EntityNotFoundException("Part not found: " + partId));
        part.consumeStock(1);
        inventoryRepository.save(part);
    }
}
