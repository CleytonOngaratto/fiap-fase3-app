package br.com.fiap.postech.carworkshop.autoservice.adapter.presenter;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;

import java.math.BigDecimal;

public record AutoServiceResponse(Long id, String description, BigDecimal price) {

    public static AutoServiceResponse from(AutoService autoService) {
        return new AutoServiceResponse(autoService.getId(), autoService.getDescription(), autoService.getPrice());
    }
}
