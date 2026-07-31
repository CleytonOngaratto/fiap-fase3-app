package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

/**
 * OUT-port for completion notifications. Pure Java (no {@code jakarta.*}) so the use case stays
 * framework-agnostic (D4/V4/V2): the interactor depends on this abstraction, while the CDI wiring
 * and the actual side-effect live in the infrastructure adapter that implements it.
 */
public interface WorkOrderNotificationPort {

    void notifyCompleted(CompletedNotification notification);

    record CompletedNotification(Long workOrderId, Long customerId, String customerEmail) {}
}
