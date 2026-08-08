package br.com.fiap.postech.carworkshop.autoservice.usecase.port.out;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;

import java.util.List;
import java.util.Optional;

public interface AutoServiceRepositoryPort {
    List<AutoService> findAll();
    Optional<AutoService> findById(Long id);
    List<AutoService> findByIds(List<Long> ids);
    AutoService save(AutoService autoService);
    boolean deleteById(Long id);
}
