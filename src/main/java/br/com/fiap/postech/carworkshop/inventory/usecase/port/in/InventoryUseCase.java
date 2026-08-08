package br.com.fiap.postech.carworkshop.inventory.usecase.port.in;

import br.com.fiap.postech.carworkshop.inventory.adapter.dto.PartsAndSupplyRequest;
import br.com.fiap.postech.carworkshop.inventory.adapter.presenter.PartsAndSupplyResponse;

import java.util.List;

public interface InventoryUseCase {
    List<PartsAndSupplyResponse> findAll();
    PartsAndSupplyResponse findById(Long id);
    PartsAndSupplyResponse create(PartsAndSupplyRequest request);
    void update(Long id, PartsAndSupplyRequest request);
    void delete(Long id);
    void consumeStock(Long id, Integer quantity);
}
