package br.com.fiap.postech.carworkshop.workorder.infrastructure.metrics;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkOrderMetricsAdapterTest {

    private MeterRegistry registry;
    private WorkOrderMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        adapter = new WorkOrderMetricsAdapter(registry);
    }

    @Test
    void recordStatusChange_countsPerStatus() {
        adapter.recordStatusChange(StatusWO.UNDER_DIAGNOSIS);
        adapter.recordStatusChange(StatusWO.IN_PROGRESS);
        adapter.recordStatusChange(StatusWO.IN_PROGRESS);

        assertEquals(1.0, counterFor(StatusWO.UNDER_DIAGNOSIS));
        assertEquals(2.0, counterFor(StatusWO.IN_PROGRESS));
    }

    @Test
    void recordStatusChange_keepsEachStatusOnItsOwnSeries() {
        adapter.recordStatusChange(StatusWO.COMPLETED);

        assertEquals(1.0, counterFor(StatusWO.COMPLETED));
        assertNull(registry.find(WorkOrderMetricsAdapter.STATUS_CHANGES)
                .tag("status", StatusWO.DELIVERED.name()).counter());
    }

    @Test
    void recordStatusChange_ignoresNull() {
        adapter.recordStatusChange(null);

        assertNull(registry.find(WorkOrderMetricsAdapter.STATUS_CHANGES).counter());
    }

    @Test
    void recordCompletion_recordsElapsedTime() {
        adapter.recordCompletion(Duration.ofHours(3));

        assertEquals(1L, registry.get(WorkOrderMetricsAdapter.COMPLETION_TIME).timer().count());
        assertEquals(3.0,
                registry.get(WorkOrderMetricsAdapter.COMPLETION_TIME).timer().totalTime(TimeUnit.HOURS),
                0.0001);
    }

    @Test
    void recordCompletion_ignoresNull() {
        adapter.recordCompletion(null);

        assertNull(registry.find(WorkOrderMetricsAdapter.COMPLETION_TIME).timer());
    }

    @Test
    void recordCompletion_ignoresNegativeDuration() {
        adapter.recordCompletion(Duration.ofMinutes(-30));

        assertNull(registry.find(WorkOrderMetricsAdapter.COMPLETION_TIME).timer());
    }

    private double counterFor(StatusWO status) {
        return registry.get(WorkOrderMetricsAdapter.STATUS_CHANGES)
                .tag("status", status.name()).counter().count();
    }
}
