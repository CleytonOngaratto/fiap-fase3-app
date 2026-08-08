package br.com.fiap.postech.carworkshop.autoservice.usecase.interactor;

import br.com.fiap.postech.carworkshop.autoservice.adapter.dto.AutoServiceRequest;
import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.autoservice.usecase.interactor.AutoServiceInteractor;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.out.AutoServiceRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
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

class AutoServiceInteractorTest {

    @InjectMocks
    AutoServiceInteractor interactor;

    @Mock
    AutoServiceRepositoryPort repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAll_shouldReturnList() {
        AutoService entity = AutoService.builder().id(1L).description("Oil Change").price(new BigDecimal("99.99")).build();
        when(repository.findAll()).thenReturn(List.of(entity));

        List<AutoServiceResponse> result = interactor.findAll();

        assertEquals(1, result.size());
        assertEquals("Oil Change", result.get(0).description());
        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnResponse() {
        AutoService entity = AutoService.builder().id(1L).description("Oil Change").price(new BigDecimal("99.99")).build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        AutoServiceResponse result = interactor.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(repository).findById(1L);
    }

    @Test
    void findById_shouldThrowNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.findById(1L));
    }

    @Test
    void create_shouldPersistAndReturnResponse() {
        AutoService saved = AutoService.builder().id(1L).description("Oil Change").price(new BigDecimal("99.99")).build();
        when(repository.save(any(AutoService.class))).thenReturn(saved);

        AutoServiceRequest request = new AutoServiceRequest("Oil Change", new BigDecimal("99.99"));
        AutoServiceResponse result = interactor.create(request);

        assertNotNull(result);
        assertEquals("Oil Change", result.description());
        verify(repository).save(any(AutoService.class));
    }

    @Test
    void update_shouldUpdateSuccessfully() {
        AutoService existing = AutoService.builder().id(1L).description("Oil Change").price(new BigDecimal("99.99")).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(AutoService.class))).thenReturn(existing);

        assertDoesNotThrow(() -> interactor.update(1L, new AutoServiceRequest("New Name", new BigDecimal("120.00"))));
        verify(repository).save(any(AutoService.class));
    }

    @Test
    void update_shouldThrowNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.update(1L, new AutoServiceRequest("X", BigDecimal.ONE)));
    }

    @Test
    void delete_shouldDeleteSuccessfully() {
        when(repository.deleteById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> interactor.delete(1L));
    }

    @Test
    void delete_shouldThrowNotFound() {
        when(repository.deleteById(1L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> interactor.delete(1L));
    }

    @Test
    void findById_throwsValidationForNullId() {
        assertThrows(ValidationException.class, () -> interactor.findById(null));
        verifyNoInteractions(repository);
    }

    @Test
    void findById_throwsValidationForZeroId() {
        assertThrows(ValidationException.class, () -> interactor.findById(0L));
        verifyNoInteractions(repository);
    }

    @Test
    void delete_throwsValidationForInvalidId() {
        assertThrows(ValidationException.class, () -> interactor.delete(0L));
        verifyNoInteractions(repository);
    }
}
