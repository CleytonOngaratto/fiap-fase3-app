package br.com.fiap.postech.carworkshop.workorder.domain.entity;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.shared.domain.exception.InvalidOperationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    private Long id;
    private Long customerId;
    private String customerEmail;
    private Long vehicleId;
    private String vehiclePlate;
    private StatusWO status;
    private LocalDateTime creationDate;
    private LocalDateTime endDate;
    private String diagnosticDescription;
    private BigDecimal budgetValue;
    private LocalDateTime budgetApprovalDate;
    private List<AutoService> services;
    private List<PartsAndSupply> parts;
    private boolean deleted;

    public static WorkOrder create(Long customerId, String customerEmail,
                                   Long vehicleId, String vehiclePlate,
                                   List<AutoService> services, List<PartsAndSupply> parts) {
        return WorkOrder.builder()
                .customerId(customerId)
                .customerEmail(customerEmail)
                .vehicleId(vehicleId)
                .vehiclePlate(vehiclePlate)
                .services(services)
                .parts(parts != null ? parts : List.of())
                .status(StatusWO.RECEIVED)
                .creationDate(LocalDateTime.now())
                .build();
    }

    public void generateBudget() {
        BigDecimal total = BigDecimal.ZERO;
        for (AutoService service : this.services) {
            total = total.add(service.getPrice());
        }
        if (this.parts != null) {
            for (PartsAndSupply part : this.parts) {
                total = total.add(part.getPrice());
            }
        }
        this.budgetValue = total;
        this.status = StatusWO.PENDING_APPROVAL;
    }

    public void approveBudget() {
        if (this.status != StatusWO.PENDING_APPROVAL) {
            throw new InvalidOperationException("Budget can only be approved when status is PENDING_APPROVAL.");
        }
        this.status = StatusWO.IN_PROGRESS;
        this.budgetApprovalDate = LocalDateTime.now();
    }

    public void rejectBudget() {
        if (this.status != StatusWO.PENDING_APPROVAL) {
            throw new InvalidOperationException("Budget can only be rejected when status is PENDING_APPROVAL.");
        }
        this.status = StatusWO.CANCELED;
    }

    public void completeService() {
        if (this.status != StatusWO.IN_PROGRESS) {
            throw new InvalidOperationException("Work order can only be completed when status is IN_PROGRESS.");
        }
        this.status = StatusWO.COMPLETED;
        this.endDate = LocalDateTime.now();
    }

    public void deliverVehicle() {
        if (this.status != StatusWO.COMPLETED) {
            throw new InvalidOperationException("Vehicle can only be delivered when status is COMPLETED.");
        }
        this.status = StatusWO.DELIVERED;
    }

    public void softDelete() {
        if (this.status != StatusWO.COMPLETED && this.status != StatusWO.DELIVERED) {
            throw new InvalidOperationException("Only COMPLETED or DELIVERED work orders can be deleted.");
        }
        this.deleted = true;
    }
}
