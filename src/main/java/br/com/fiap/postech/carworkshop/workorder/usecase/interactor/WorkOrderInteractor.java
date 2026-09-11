package br.com.fiap.postech.carworkshop.workorder.usecase.interactor;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.DiagnosisRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.dto.WorkOrderRequest;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.DiagnosisResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderDetailResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderResponse;
import br.com.fiap.postech.carworkshop.workorder.adapter.presenter.WorkOrderTrackingResponse;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.domain.entity.WorkOrder;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.AutoServiceDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.CustomerDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.InventoryDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.VehicleDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderMetricsPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderNotificationPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderRepositoryPort;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
public class WorkOrderInteractor implements WorkOrderUseCase {

    private final WorkOrderRepositoryPort workOrderRepository;
    private final CustomerDataPort customerDataPort;
    private final VehicleDataPort vehicleDataPort;
    private final AutoServiceDataPort autoServiceDataPort;
    private final InventoryDataPort inventoryDataPort;
    private final WorkOrderNotificationPort notificationPort;
    private final WorkOrderMetricsPort metricsPort;

    public WorkOrderInteractor(WorkOrderRepositoryPort workOrderRepository,
                               CustomerDataPort customerDataPort,
                               VehicleDataPort vehicleDataPort,
                               AutoServiceDataPort autoServiceDataPort,
                               InventoryDataPort inventoryDataPort,
                               WorkOrderNotificationPort notificationPort,
                               WorkOrderMetricsPort metricsPort) {
        this.workOrderRepository = workOrderRepository;
        this.customerDataPort = customerDataPort;
        this.vehicleDataPort = vehicleDataPort;
        this.autoServiceDataPort = autoServiceDataPort;
        this.inventoryDataPort = inventoryDataPort;
        this.notificationPort = notificationPort;
        this.metricsPort = metricsPort;
    }

    @Override
    public WorkOrderResponse create(WorkOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new ValidationException("customer_id is required.");
        }
        if (request.getVehicleId() == null) {
            throw new ValidationException("vehicle_id is required.");
        }
        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            throw new ValidationException("A work order must have at least one service.");
        }

        CustomerDataPort.CustomerInfo customer = customerDataPort.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));

        VehicleDataPort.VehicleInfo vehicle = vehicleDataPort.findById(request.getVehicleId())
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found."));

        List<AutoService> services = autoServiceDataPort.findByIds(request.getServiceIds());
        if (services.size() != request.getServiceIds().size()) {
            throw new EntityNotFoundException("One or more services not found.");
        }

        List<PartsAndSupply> parts = Collections.emptyList();
        if (request.getPartsIds() != null && !request.getPartsIds().isEmpty()) {
            parts = inventoryDataPort.findByIds(request.getPartsIds());
            if (parts.size() != request.getPartsIds().size()) {
                throw new EntityNotFoundException("One or more parts not found.");
            }
        }

        WorkOrder workOrder = WorkOrder.create(
                customer.id(), customer.email(),
                vehicle.id(), vehicle.vehiclePlate(),
                services, parts
        );
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work Order {} created.", saved.getId());
        recordTransition(saved);
        return WorkOrderResponse.from(saved);
    }

    private static final Map<StatusWO, Integer> STATUS_PRIORITY = Map.of(
            StatusWO.IN_PROGRESS, 1,
            StatusWO.PENDING_APPROVAL, 2,
            StatusWO.UNDER_DIAGNOSIS, 3,
            StatusWO.RECEIVED, 4
    );

    @Override
    public List<WorkOrderDetailResponse> findAll() {
        return workOrderRepository.findAll().stream().map(WorkOrderDetailResponse::from).toList();
    }

    @Override
    public List<WorkOrderDetailResponse> findAllActive() {
        return workOrderRepository.findAllActive().stream()
                .sorted(Comparator.comparingInt((WorkOrder wo) ->
                        STATUS_PRIORITY.getOrDefault(wo.getStatus(), 99))
                        .thenComparing(WorkOrder::getCreationDate))
                .map(WorkOrderDetailResponse::from)
                .toList();
    }

    @Override
    public void deleteWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
        workOrder.softDelete();
        workOrderRepository.save(workOrder);
        log.info("Work Order {} soft-deleted.", id);
    }

    @Override
    public WorkOrderDetailResponse findById(Long id) {
        return workOrderRepository.findById(id)
                .map(WorkOrderDetailResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
    }

    @Override
    public DiagnosisResponse completeDiagnosis(Long id, DiagnosisRequest request) {
        if (request.getDiagnosticDescription() == null || request.getDiagnosticDescription().isBlank()) {
            throw new ValidationException("diagnostic_description is required.");
        }
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));

        if (workOrder.getStatus() != StatusWO.UNDER_DIAGNOSIS) {
            String msg = switch (workOrder.getStatus()) {
                case RECEIVED -> "Work Order is awaiting diagnosis scheduling.";
                default -> "Diagnosis for this Work Order is in an invalid state: " + workOrder.getStatus();
            };
            throw new ValidationException(msg);
        }
        workOrder.setDiagnosticDescription(request.getDiagnosticDescription());
        workOrder.generateBudget();
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Diagnosis for Work Order {} completed. Budget: R$ {}", id, saved.getBudgetValue());
        recordTransition(saved);
        return DiagnosisResponse.from(saved);
    }

    @Override
    public WorkOrderDetailResponse completeWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
        workOrder.completeService();
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work Order {} completed.", id);
        recordTransition(saved);
        metricsPort.recordCompletion(Duration.between(saved.getCreationDate(), saved.getEndDate()));
        notificationPort.notifyCompleted(new WorkOrderNotificationPort.CompletedNotification(
                saved.getId(), saved.getCustomerId(), saved.getCustomerEmail()));
        return WorkOrderDetailResponse.from(saved);
    }

    @Override
    public WorkOrderDetailResponse deliverWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
        workOrder.deliverVehicle();
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work Order {} delivered.", id);
        recordTransition(saved);
        return WorkOrderDetailResponse.from(saved);
    }

    @Override
    public WorkOrderTrackingResponse findForCustomer(Long id) {
        return workOrderRepository.findById(id)
                .map(WorkOrderTrackingResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
    }

    @Override
    public WorkOrderTrackingResponse approveWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
        workOrder.approveBudget();
        workOrder.getParts().forEach(part -> inventoryDataPort.consumeStock(part.getId()));
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work Order {} approved. Status: {}", id, saved.getStatus());
        recordTransition(saved);
        return WorkOrderTrackingResponse.from(saved);
    }

    @Override
    public WorkOrderTrackingResponse rejectWorkOrder(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Work Order not found."));
        workOrder.rejectBudget();
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Work Order {} rejected. Status: {}", id, saved.getStatus());
        recordTransition(saved);
        return WorkOrderTrackingResponse.from(saved);
    }

    @Override
    public Double getAverageCompletionTimeInHours() {
        List<WorkOrder> completed = workOrderRepository.findCompleted();
        if (completed.isEmpty()) return 0.0;
        double totalMinutes = completed.stream()
                .mapToLong(wo -> ChronoUnit.MINUTES.between(wo.getCreationDate(), wo.getEndDate()))
                .sum();
        return totalMinutes / 60.0 / completed.size();
    }

    // Toda transição alimenta as duas métricas: o contador (quantas OS por status) e o timer (quanto
    // tempo a OS levou para chegar nele). Juntas é o que os dashboards consultam.
    private void recordTransition(WorkOrder workOrder) {
        metricsPort.recordStatusChange(workOrder.getStatus());
        metricsPort.recordTimeToStatus(
                workOrder.getStatus(),
                Duration.between(workOrder.getCreationDate(), LocalDateTime.now()));
    }
}
