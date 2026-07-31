package br.com.fiap.postech.carworkshop.vehicle.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code ignoreUnknown = false}: a misspelled field (e.g. {@code "customer_id"} instead of
 * {@code "customerId"}) is rejected with a 400 naming the bad field, instead of being silently
 * dropped to {@code null} — which previously let a vehicle be created without an owner (violates D2).
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record VehicleRequest(String vehiclePlate, String manufacturer, String modelName, Integer modelYear,
                              Long customerId) {}
