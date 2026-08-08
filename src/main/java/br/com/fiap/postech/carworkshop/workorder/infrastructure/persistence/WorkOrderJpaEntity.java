package br.com.fiap.postech.carworkshop.workorder.infrastructure.persistence;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "WorkOrderJpaEntity")
@Table(name = "work_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderJpaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_plate")
    private String vehiclePlate;

    @Enumerated(EnumType.STRING)
    private StatusWO status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "diagnostic_description")
    private String diagnosticDescription;

    @Column(name = "budget_value")
    private BigDecimal budgetValue;

    @Column(name = "budget_approval_date")
    private LocalDateTime budgetApprovalDate;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "work_order_services",
            joinColumns = @JoinColumn(name = "work_order_id"))
    private List<WorkOrderServiceSnapshot> services;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "work_order_parts",
            joinColumns = @JoinColumn(name = "work_order_id"))
    private List<WorkOrderPartSnapshot> parts;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
