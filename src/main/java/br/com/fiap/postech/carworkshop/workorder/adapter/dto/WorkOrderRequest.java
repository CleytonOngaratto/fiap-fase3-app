package br.com.fiap.postech.carworkshop.workorder.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WorkOrderRequest {

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("vehicle_id")
    private Long vehicleId;

    @JsonProperty("service_ids")
    private List<Long> serviceIds;

    @JsonProperty("parts_ids")
    private List<Long> partsIds;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public List<Long> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<Long> serviceIds) { this.serviceIds = serviceIds; }
    public List<Long> getPartsIds() { return partsIds; }
    public void setPartsIds(List<Long> partsIds) { this.partsIds = partsIds; }
}
