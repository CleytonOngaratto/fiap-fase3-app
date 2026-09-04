package br.com.fiap.postech.carworkshop.autoservice.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "auto_service")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoServiceJpaEntity extends PanacheEntityBase {

    @Id
    // allocationSize = 1 matches the sequence's INCREMENT BY 1; the default of 50 overlaps blocks.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auto_service_seq")
    @SequenceGenerator(name = "auto_service_seq", sequenceName = "auto_service_seq", allocationSize = 1)
    public Long id;

    private String description;
    private BigDecimal price;
}
