package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

import br.com.fiap.postech.carworkshop.workorder.domain.entity.StatusWO;

import java.time.Duration;

/**
 * Java puro de propósito: a regra ArchUnit {@code usecase -> infrastructure} é estrita, então o
 * Micrometer fica do lado do adapter.
 */
public interface WorkOrderMetricsPort {

    void recordStatusChange(StatusWO status);

    /** {@code serviceDuration} é criação → conclusão: o "tempo médio" sem timestamp por status. */
    void recordCompletion(Duration serviceDuration);
}
