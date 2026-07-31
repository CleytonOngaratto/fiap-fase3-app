package br.com.fiap.postech.carworkshop.customer.usecase.interactor;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.customer.adapter.presenter.CustomerResponse;
import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;
import br.com.fiap.postech.carworkshop.customer.usecase.interactor.CustomerInteractor;
import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerInteractorTest {

    @InjectMocks
    CustomerInteractor interactor;

    @Mock
    CustomerRepositoryPort repository;

    private Customer customerDomain;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customerDomain = Customer.builder()
                .id(1L).name("João Silva").document("12345678909")
                .rg("MG1234567").email("joao@email.com").number("11999999999")
                .build();
        customerRequest = new CustomerRequest("João Silva", "12345678909", "MG1234567",
                "joao@email.com", "11999999999");
    }

    @Test
    void findAll_shouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(customerDomain));
        List<CustomerResponse> result = interactor.findAll();
        assertEquals(1, result.size());
        assertEquals("João Silva", result.get(0).name());
        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnCustomer() {
        when(repository.findById(1L)).thenReturn(Optional.of(customerDomain));
        CustomerResponse result = interactor.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void findById_shouldThrowNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.findById(1L));
    }

    @Test
    void findByDocument_shouldReturnCustomer() {
        when(repository.findByDocument("12345678909")).thenReturn(Optional.of(customerDomain));
        CustomerResponse result = interactor.findByDocument("12345678909");
        assertNotNull(result);
        assertEquals("12345678909", result.document());
    }

    @Test
    void findByDocument_blankDoc_throwsValidation() {
        assertThrows(ValidationException.class, () -> interactor.findByDocument(""));
        verifyNoInteractions(repository);
    }

    @Test
    void create_shouldPersistAndReturn() {
        when(repository.findByDocument(any())).thenReturn(Optional.empty());
        when(repository.findByEmail(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(customerDomain);

        CustomerResponse result = interactor.create(customerRequest);
        assertNotNull(result);
        verify(repository).save(any(Customer.class));
    }

    @Test
    void create_duplicateDocument_throwsValidation() {
        when(repository.findByDocument("12345678909")).thenReturn(Optional.of(customerDomain));
        assertThrows(ValidationException.class, () -> interactor.create(customerRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void update_shouldUpdateSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(customerDomain));
        when(repository.findByDocument(any())).thenReturn(Optional.empty());
        when(repository.findByEmail(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(customerDomain);

        assertDoesNotThrow(() -> interactor.update(1L, customerRequest));
        verify(repository).save(any());
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> interactor.update(1L, customerRequest));
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
    void delete_invalidId_throwsValidation() {
        assertThrows(ValidationException.class, () -> interactor.delete(0L));
        verifyNoInteractions(repository);
    }
}
