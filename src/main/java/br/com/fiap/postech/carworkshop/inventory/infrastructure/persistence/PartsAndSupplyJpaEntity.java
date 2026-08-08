package br.com.fiap.postech.carworkshop.inventory.infrastructure.persistence;

import br.com.fiap.postech.carworkshop.inventory.domain.entity.TypeProductEnum;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "parts_and_supply")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartsAndSupplyJpaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    private String code;
    private String manufacturer;
    private String description;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private TypeProductEnum type;

    private Integer quantity;
}
