package br.com.fiap.postech.carworkshop.auth.infrastructure.adapter;

import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;
import br.com.fiap.postech.carworkshop.auth.usecase.port.out.TokenProviderPort;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class JwtTokenAdapter implements TokenProviderPort {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @Override
    public String generateToken(String username, List<UserRoles> roles) {
        return Jwt.claims()
                .subject(username)
                .issuer(issuer)
                .groups(roles.stream().map(UserRoles::name).collect(Collectors.toSet()))
                .expiresIn(3600)
                .sign();
    }
}
