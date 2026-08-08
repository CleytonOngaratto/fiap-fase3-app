package br.com.fiap.postech.carworkshop.auth.usecase.port.in;

import br.com.fiap.postech.carworkshop.auth.adapter.dto.LoginRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.dto.SignupRequest;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.AuthTokenResponse;
import br.com.fiap.postech.carworkshop.auth.adapter.presenter.UserResponse;

public interface AuthUseCase {
    UserResponse signup(SignupRequest request);
    AuthTokenResponse login(LoginRequest request);
}
