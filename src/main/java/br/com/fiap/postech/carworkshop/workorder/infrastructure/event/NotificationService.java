package br.com.fiap.postech.carworkshop.workorder.infrastructure.event;

import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderNotificationPort;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
real e-mail provider needed.
 */
@Slf4j
@ApplicationScoped
public class NotificationService implements WorkOrderNotificationPort {

    @Override
    public void notifyCompleted(CompletedNotification notification) {
        log.info("Work Order {} completed. Notifying customer {} via {}...",
                notification.workOrderId(), notification.customerId(), notification.customerEmail());
        log.info("Simulation: Email sent to {} for Work Order {}",
                notification.customerEmail(), notification.workOrderId());
    }
}
