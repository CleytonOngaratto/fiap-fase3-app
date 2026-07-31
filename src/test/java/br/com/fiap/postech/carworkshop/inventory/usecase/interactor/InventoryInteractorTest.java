package br.com.fiap.postech.carworkshop.inventory.usecase.interactor;

import br.com.fiap.postech.carworkshop.inventory.adapter.dto.PartsAndSupplyRequest;
import br.com.fiap.postech.carworkshop.inventory.adapter.presenter.PartsAndSupplyResponse;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.PartsAndSupply;
import br.com.fiap.postech.carworkshop.inventory.domain.entity.TypeProductEnum;
import br.com.fiap.postech.carworkshop.inventory.usecase.interactor.InventoryInteractor;
import br.com.fiap.postech.carworkshop.inventory.usecase.port.out.InventoryRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.StockException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryInteractorTest {

    @InjectMocks
    InventoryInteractor interactor;

    @Mock
    InventoryRepositoryPort repository;

    private PartsAndSupply partsDomain;
    private PartsAndSupplyRequest partsRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        partsDomain = PartsAndSupply.builder().id(1L).code("P001").manufacturer("Bosch")
                .description("Oil Filter").price(new BigDecimal("35.90"))
                .type(TypeProductEnum.UNITARY).quantity(10).build();
        partsRequest = new PartsAndSupplyRequest("P001", "Bosch", "Oil Filter",
                new BigDecimal("35.90"), TypeProductEnum.UNITARY, 10);
    }

    @Test
    void findAll_shouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(partsDomain));
        List<PartsAndSupplyResponse> result = interactor.findAll();
        assertEquals(1, result.size());
        assertEquals("P001", result.get(0).code());
    }

    @Test
    void findById_shouldReturn() {
        when(repository.findById(1L)).thenReturn(Optional.of(partsDomain));
        PartsAndSupplyResponse result = interactor.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void findById_notFound_throwsEntityNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.findById(1L));
    }

    @Test
    void findById_invalidId_throwsValidation() {
        assertThrows(ValidationException.class, () -> interactor.findById(0L));
        verifyNoInteractions(repository);
    }

    @Test
    void create_shouldPersistAndReturn() {
        when(repository.save(any())).thenReturn(partsDomain);
        PartsAndSupplyResponse result = interactor.create(partsRequest);
        assertNotNull(result);
        verify(repository).save(any(PartsAndSupply.class));
    }

    @Test
    void update_shouldUpdateSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(partsDomain));
        when(repository.save(any())).thenReturn(partsDomain);
        assertDoesNotThrow(() -> interactor.update(1L, partsRequest));
        verify(repository).save(any());
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.update(1L, partsRequest));
    }

    @Test
    void delete_shouldDeleteSuccessfully() {
        when(repository.deleteById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> interactor.delete(1L));
    }

    @Test
    void delete_notFound_throwsEntityNotFound() {
        when(repository.deleteById(1L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> interactor.delete(1L));
    }

    @Test
    void consumeStock_shouldDeductQuantity() {
        when(repository.findById(1L)).thenReturn(Optional.of(partsDomain));
        when(repository.save(any())).thenReturn(partsDomain);
        assertDoesNotThrow(() -> interactor.consumeStock(1L, 5));
        assertEquals(5, partsDomain.getQuantity());
    }

    @Test
    void consumeStock_insufficientStock_throwsStockException() {
        partsDomain.setQuantity(2);
        when(repository.findById(1L)).thenReturn(Optional.of(partsDomain));
        assertThrows(StockException.class, () -> interactor.consumeStock(1L, 5));
    }

    @Test
    void consumeStock_invalidQuantity_throwsValidation() {
        assertThrows(ValidationException.class, () -> interactor.consumeStock(1L, 0));
        verifyNoInteractions(repository);
    }

    @Test
    void delete_invalidId_throwsValidation() {
        assertThrows(ValidationException.class, () -> interactor.delete(0L));
        verifyNoInteractions(repository);
    }
}
