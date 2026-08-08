package br.com.fiap.postech.carworkshop.workorder.adapter.gateway;

import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.CustomerDataPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class CustomerDataGateway implements CustomerDataPort {

    @Inject
    CustomerRepositoryPort customerRepository;

    @Override
    public Optional<CustomerInfo> findById(Long id) {
        return customerRepository.findById(id)
                .map(customer -> new CustomerInfo(customer.getId(), customer.getEmail()));
    }
}
