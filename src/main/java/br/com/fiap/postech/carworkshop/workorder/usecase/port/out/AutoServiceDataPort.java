package br.com.fiap.postech.carworkshop.workorder.usecase.port.out;

import br.com.fiap.postech.carworkshop.autoservice.domain.entity.AutoService;

import java.util.List;

public interface AutoServiceDataPort {
    List<AutoService> findByIds(List<Long> ids);
}
