package br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderServiceSnapshot {

    @Column(name = "auto_service_id")
    private Long autoServiceId;

    private String description;

    private BigDecimal price;
}
