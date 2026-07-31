package br.com.fiap.postech.carworkshop.autoservice.usecase.interactor;

import br.com.fiap.postech.carworkshop.autoservice.adapter.dto.AutoServiceRequest;
import br.com.fiap.postech.carworkshop.autoservice.adapter.presenter.AutoServiceResponse;
import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.in.AutoServiceUseCase;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.out.AutoServiceRepositoryPort;
import br.com.fiap.postech.carworkshop.shared.domain.exception.EntityNotFoundException;
import br.com.fiap.postech.carworkshop.shared.domain.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class AutoServiceInteractor implements AutoServiceUseCase {

    private final AutoServiceRepositoryPort repository;

    public AutoServiceInteractor(AutoServiceRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<AutoServiceResponse> findAll() {
        log.info("Finding all auto services");
        return repository.findAll().stream().map(AutoServiceResponse::from).toList();
    }

    @Override
    public AutoServiceResponse findById(Long id) {
        log.info("Finding auto service by id: {}", id);
        validateId(id);
        return repository.findById(id)
                .map(AutoServiceResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));
    }

    @Override
    public AutoServiceResponse create(AutoServiceRequest request) {
        log.info("Creating auto service: {}", request);
        validateRequest(request);
        AutoService autoService = AutoService.builder()
                .description(request.description())
                .price(request.price())
                .build();
        AutoService saved = repository.save(autoService);
        return AutoServiceResponse.from(saved);
    }

    @Override
    public void update(Long id, AutoServiceRequest request) {
        log.info("Updating auto service id: {}", id);
        validateId(id);
        validateRequest(request);
        AutoService existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting auto service id: {}", id);
        validateId(id);
        if (!repository.deleteById(id)) {
            throw new EntityNotFoundException("Service not found");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("Invalid ID: must be a positive number.");
        }
    }

    private void validateRequest(AutoServiceRequest request) {
        if (request.description() == null || request.description().isBlank()) {
            throw new ValidationException("Service description must not be blank.");
        }
        if (request.price() == null || request.price().signum() <= 0) {
            throw new ValidationException("Service price must be greater than zero.");
        }
    }
}
