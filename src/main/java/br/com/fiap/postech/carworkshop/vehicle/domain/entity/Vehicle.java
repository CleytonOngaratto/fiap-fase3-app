package br.com.fiap.postech.carworkshop.vehicle.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private Long id;
    private String vehiclePlate;
    private String manufacturer;
    private String modelName;
    private Integer modelYear;
    private Long customerId;
}
