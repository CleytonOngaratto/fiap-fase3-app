package br.com.fiap.postech.carworkshop.workorder.infrastructure.metrics;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;

@ApplicationScoped
public class WorkOrderMetricsAdapter implements WorkOrderMetricsPort {

    static final String STATUS_CHANGES = "workorder.status.changes";
    static final String COMPLETION_TIME = "workorder.completion.time";

    private final MeterRegistry registry;

    public WorkOrderMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordStatusChange(StatusWO status) {
        if (status == null) {
            return;
        }
        registry.counter(STATUS_CHANGES, "status", status.name()).increment();
    }

    @Override
    public void recordCompletion(Duration serviceDuration) {
        // Clock skew ou endDate editada à mão geram span negativo, que o Micrometer registra
        // sem reclamar e distorce a média.
        if (serviceDuration == null || serviceDuration.isNegative()) {
            return;
        }
        Timer.builder(COMPLETION_TIME)
                .description("Elapsed time from work order creation to service completion")
                .register(registry)
                .record(serviceDuration);
    }
}
