package br.com.fiap.postech.carworkshop.workorder.infrastructure.config;

import br.com.fiap.postech.carworkshop.workorder.infrastructure.config.WorkOrderModuleConfig;
import br.com.fiap.postech.carworkshop.workorder.usecase.interactor.WorkOrderInteractor;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.in.WorkOrderUseCase;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.AutoServiceDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.CustomerDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.InventoryDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.VehicleDataPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderMetricsPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderNotificationPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class WorkOrderModuleConfigTest {

    @Test
    void workOrderUseCase_buildsWiredInteractor() {
        WorkOrderUseCase useCase = new WorkOrderModuleConfig().workOrderUseCase(
                mock(WorkOrderRepositoryPort.class),
                mock(CustomerDataPort.class),
                mock(VehicleDataPort.class),
                mock(AutoServiceDataPort.class),
                mock(InventoryDataPort.class),
                mock(WorkOrderNotificationPort.class),
                mock(WorkOrderMetricsPort.class));

        assertNotNull(useCase);
        assertInstanceOf(WorkOrderInteractor.class, useCase);
    }
}
