package br.com.fiap.postech.carworkshop.customer.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerJpaEntity extends PanacheEntityBase {

    @Id
    // allocationSize = 1 matches the sequence's INCREMENT BY 1; the default of 50 overlaps blocks.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customers_seq")
    @SequenceGenerator(name = "customers_seq", sequenceName = "customers_seq", allocationSize = 1)
    public Long id;

    private String name;

    @Column(name = "document")
    private String document;

    private String rg;
    private String email;
    private String number;
}
