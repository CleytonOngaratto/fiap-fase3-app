package br.com.fiap.postech.carworkshop.inventory.adapter.presenter;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.TypeProductEnum;

import java.math.BigDecimal;

public record PartsAndSupplyResponse(Long id, String code, String manufacturer, String description,
                                      BigDecimal price, TypeProductEnum type, Integer quantity) {

    public static PartsAndSupplyResponse from(PartsAndSupply p) {
        return new PartsAndSupplyResponse(p.getId(), p.getCode(), p.getManufacturer(),
                p.getDescription(), p.getPrice(), p.getType(), p.getQuantity());
    }
}
