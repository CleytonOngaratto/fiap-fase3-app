package br.com.fiap.postech.carworkshop.auth.adapter.dto;

import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;

import java.util.List;

public record SignupRequest(String username, String password, List<UserRoles> roles) {}
