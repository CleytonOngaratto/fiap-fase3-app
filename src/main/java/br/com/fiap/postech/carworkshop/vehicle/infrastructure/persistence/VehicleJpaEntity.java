package br.com.fiap.postech.carworkshop.vehicle.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleJpaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    private String vehiclePlate;
    private String manufacturer;
    private String modelName;
    private Integer modelYear;

    // D2: decoupled from CustomerJpaEntity — owner is a plain logical reference (no @ManyToOne FK),
    // kept in the existing owner_id column. "Vehicle always has an owner" is enforced in the use case.
    @Column(name = "owner_id")
    private Long customerId;
}
