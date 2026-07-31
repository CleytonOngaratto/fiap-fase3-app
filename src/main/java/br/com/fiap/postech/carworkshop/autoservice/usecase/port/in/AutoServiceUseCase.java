package br.com.fiap.postech.carworkshop.autoservice.usecase.port.in;

import br.com.fiap.postech.carworkshop.autoservice.adapter.dto.AutoServiceRequest;
import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;

import java.util.List;

public interface AutoServiceUseCase {
    List<AutoServiceResponse> findAll();
    AutoServiceResponse findById(Long id);
    AutoServiceResponse create(AutoServiceRequest request);
    void update(Long id, AutoServiceRequest request);
    void delete(Long id);
}
