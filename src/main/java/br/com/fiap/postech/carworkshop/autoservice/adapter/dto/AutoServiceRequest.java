package br.com.fiap.postech.carworkshop.autoservice.adapter.dto;

import java.math.BigDecimal;

public record AutoServiceRequest(String description, BigDecimal price) {}
