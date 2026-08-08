package br.com.fiap.postech.carworkshop.auth.usecase.port.out;

import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;

import java.util.List;

public interface TokenProviderPort {
    String generateToken(String username, List<UserRoles> roles);
}
