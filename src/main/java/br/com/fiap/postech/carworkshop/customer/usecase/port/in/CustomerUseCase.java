package br.com.fiap.postech.carworkshop.customer.usecase.port.in;

import br.com.fiap.postech.carworkshop.customer.adapter.dto.CustomerRequest;
import br.com.fiap.postech.carworkshop.customer.adapter.presenter.CustomerResponse;

import java.util.List;

public interface CustomerUseCase {
    List<CustomerResponse> findAll();
    CustomerResponse findById(Long id);
    CustomerResponse findByDocument(String document);
    CustomerResponse findByEmail(String email);
    CustomerResponse create(CustomerRequest request);
    void update(Long id, CustomerRequest request);
    void delete(Long id);
}
