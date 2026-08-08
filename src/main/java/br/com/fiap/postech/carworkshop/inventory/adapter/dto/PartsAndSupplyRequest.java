package br.com.fiap.postech.carworkshop.inventory.adapter.dto;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.TypeProductEnum;

import java.math.BigDecimal;

public record PartsAndSupplyRequest(String code, String manufacturer, String description,
                                     BigDecimal price, TypeProductEnum type, Integer quantity) {}
