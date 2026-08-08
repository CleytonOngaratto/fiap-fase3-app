package br.com.fiap.postech.carworkshop.auth.adapter.presenter;

import br.com.fiap.postech.carworkshop.auth.domain.entity.User;
import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;

import java.util.List;

public record UserResponse(Long id, String username, List<UserRoles> roles) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRoles());
    }
}
