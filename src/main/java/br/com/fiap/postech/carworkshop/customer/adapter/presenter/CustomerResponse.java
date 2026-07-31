package br.com.fiap.postech.carworkshop.customer.adapter.presenter;

import br.com.fiap.postech.carworkshop.customer.domain.entity.Customer;

public record CustomerResponse(Long id, String name, String document, String rg, String email, String number) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getDocument(),
                customer.getRg(),
                customer.getEmail(),
                customer.getNumber()
        );
    }
}
