package br.com.fiap.postech.carworkshop.workorder.infrastructure.config;

import br.com.fiap.postech.carworkshop.workorder.usecase.interactor.WorkOrderInteractor;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.AutoServiceDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.CustomerDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.InventoryDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.VehicleDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderMetricsPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderNotificationPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI wiring for the work order use case. The framework annotation lives HERE so the
 * {@link WorkOrderInteractor} stays pure Java (D4/V4): no {@code jakarta.*} in the use case,
 * dependencies injected by constructor. Mirrors the VehicleModuleConfig reference pattern.
 */
@ApplicationScoped
public class WorkOrderModuleConfig {

    @Produces
    @ApplicationScoped
    public WorkOrderUseCase workOrderUseCase(WorkOrderRepositoryPort workOrderRepository,
                                             CustomerDataPort customerDataPort,
                                             VehicleDataPort vehicleDataPort,
                                             AutoServiceDataPort autoServiceDataPort,
                                             InventoryDataPort inventoryDataPort,
                                             WorkOrderNotificationPort notificationPort,
                                             WorkOrderMetricsPort metricsPort) {
        return new WorkOrderInteractor(workOrderRepository, customerDataPort, vehicleDataPort,
                autoServiceDataPort, inventoryDataPort, notificationPort, metricsPort);
    }
}
