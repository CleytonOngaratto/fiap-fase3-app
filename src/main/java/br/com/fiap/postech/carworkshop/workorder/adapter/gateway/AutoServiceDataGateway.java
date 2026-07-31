package br.com.fiap.postech.carworkshop.workorder.adapter.gateway;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;
import br.com.fiap.postech.carworkshop.autoservice.usecase.port.out.AutoServiceRepositoryPort;
import br.com.fiap.postech.carworkshop.workorder.usecase.port.out.AutoServiceDataPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class AutoServiceDataGateway implements AutoServiceDataPort {

    @Inject
    AutoServiceRepositoryPort autoServiceRepository;

    @Override
    public List<AutoService> findByIds(List<Long> ids) {
        return autoServiceRepository.findByIds(ids);
    }
}
