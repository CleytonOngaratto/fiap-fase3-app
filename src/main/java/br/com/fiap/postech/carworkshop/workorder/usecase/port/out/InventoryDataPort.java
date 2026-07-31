package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;

import java.util.List;

public interface InventoryDataPort {
    List<PartsAndSupply> findByIds(List<Long> ids);
    void consumeStock(Long partId);
}
