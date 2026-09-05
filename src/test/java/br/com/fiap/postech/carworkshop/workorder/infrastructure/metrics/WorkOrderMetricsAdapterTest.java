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

    @Test
    void recordTimeToStatus_keepsOneTimerPerStatus() {
        adapter.recordTimeToStatus(StatusWO.UNDER_DIAGNOSIS, Duration.ofMinutes(20));
        adapter.recordTimeToStatus(StatusWO.COMPLETED, Duration.ofHours(2));
        adapter.recordTimeToStatus(StatusWO.COMPLETED, Duration.ofHours(4));

        assertEquals(1L, timerFor(StatusWO.UNDER_DIAGNOSIS).count());
        assertEquals(2L, timerFor(StatusWO.COMPLETED).count());
        // A média por status é o que o dashboard consulta: 2h e 4h têm de dar 3h, não 6h.
        assertEquals(3.0, timerFor(StatusWO.COMPLETED).mean(TimeUnit.HOURS), 0.0001);
    }

    @Test
    void recordTimeToStatus_ignoresNullAndNegative() {
        adapter.recordTimeToStatus(null, Duration.ofMinutes(5));
        adapter.recordTimeToStatus(StatusWO.IN_PROGRESS, null);
        // Clock skew entre pods produz span negativo, que o Micrometer aceitaria sem reclamar.
        adapter.recordTimeToStatus(StatusWO.IN_PROGRESS, Duration.ofMinutes(-5));

        assertNull(registry.find(WorkOrderMetricsAdapter.TIME_TO_STATUS).timer());
    }

    private io.micrometer.core.instrument.Timer timerFor(StatusWO status) {
        return registry.get(WorkOrderMetricsAdapter.TIME_TO_STATUS)
                .tag("status", status.name()).timer();
    }

    private double counterFor(StatusWO status) {
        return registry.get(WorkOrderMetricsAdapter.STATUS_CHANGES)
                .tag("status", status.name()).counter().count();
    }
}
