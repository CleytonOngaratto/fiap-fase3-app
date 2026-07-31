package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

import java.util.Optional;

public interface CustomerDataPort {
    Optional<CustomerInfo> findById(Long id);

    record CustomerInfo(Long id, String email) {}
}
