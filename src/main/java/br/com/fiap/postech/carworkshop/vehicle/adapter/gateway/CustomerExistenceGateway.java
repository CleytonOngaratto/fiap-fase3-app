package br.com.fiap.postech.carworkshop.vehicle.adapter.gateway;

import br.com.fiap.postech.carworkshop.customer.usecase.port.out.CustomerRepositoryPort;
import br.com.fiap.postech.carworkshop.vehicle.usecase.port.out.CustomerExistencePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cross-module adapter (V3): checks customer existence through the customer module's PUBLIC port
 * ({@link CustomerRepositoryPort}, resolved by CDI to its public gateway) — never the customer's
 * Panache repository. Dedicated bean so {@link VehicleRepositoryGateway} keeps a single
 * responsibility (V6).
 */
@ApplicationScoped
public class CustomerExistenceGateway implements CustomerExistencePort {

    @Inject
    CustomerRepositoryPort customerRepository;

    @Override
    public boolean existsById(Long customerId) {
        return customerRepository.findById(customerId).isPresent();
    }
}
