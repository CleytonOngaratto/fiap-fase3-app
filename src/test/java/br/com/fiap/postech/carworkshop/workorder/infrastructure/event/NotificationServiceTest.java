package br.com.fiap.postech.carworkshop.workorder.infrastructure.event;

import br.com.fiap.postech.carworkshop.workorder.infrastructure.event.NotificationService;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.WorkOrderNotificationPort;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
class NotificationServiceTest {

    @Inject
    NotificationService notificationService;

    @Test
    void testNotifyCompleted_Success() {
        WorkOrderNotificationPort.CompletedNotification notification =
                new WorkOrderNotificationPort.CompletedNotification(1L, 100L, "customer@email.com");
        assertDoesNotThrow(() -> notificationService.notifyCompleted(notification));
    }
}
