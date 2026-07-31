package br.com.fiap.postech.carworkshop.vehicle.usecase.interactor;

import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import br.com.fiap.postech.carworkshop.vehicle.adapter.dto.VehicleRequest;
import br.com.fiap.postech.carworkshop.vehicle.usecase.interactor.VehicleInteractor;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.CustomerExistencePort;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.VehicleRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D3 — PREVENÇÃO do bug "veículo órfão" (PLAN.md, 2.4). Antes era um teste de <em>diagnóstico</em>
 * que afirmava o comportamento bugado; agora afirma a <b>correção</b>: criar veículo sem um dono
 * existente é rejeitado e nada é persistido. O invariante D2 ("veículo SEMPRE tem dono") é imposto
 * na camada de aplicação ({@link VehicleInteractor} + {@link CustomerExistencePort}).
 *
 * <p>A antiga "Causa B" (gateway dual-port persistindo {@code owner=null} em silêncio) deixou de
 * existir estruturalmente: o {@code VehicleRepositoryGateway} não valida mais cliente (V6), e o
 * interactor só monta/persiste o veículo após confirmar o dono.</p>
 */
class VehicleOrphanPreventionD3Test {

    @InjectMocks
    VehicleInteractor interactor;
    @Mock
    VehicleRepositoryPort repository;
    @Mock
    CustomerExistencePort customerExistencePort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /** Causa A fechada: {@code customerId} nulo é rejeitado antes de qualquer persistência. */
    @Test
    void create_withNullCustomerId_isRejected_andNothingPersisted() {
        VehicleRequest noOwner = new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2020, null);
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> interactor.create(noOwner));

        verify(customerExistencePort, never()).existsById(any());
        verify(repository, never()).save(any());
    }

    /** Cliente inexistente é rejeitado com 404 (EntityNotFound) — nada é persistido. */
    @Test
    void create_withMissingCustomer_isRejected_andNothingPersisted() {
        VehicleRequest withMissingOwner = new VehicleRequest("ABC-1234", "Toyota", "Corolla", 2020, 999L);
        when(repository.findByVehiclePlate(any())).thenReturn(Optional.empty());
        when(customerExistencePort.existsById(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> interactor.create(withMissingOwner));

        verify(repository, never()).save(any());
    }
}
