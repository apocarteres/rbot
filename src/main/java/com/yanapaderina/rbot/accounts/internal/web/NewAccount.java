package com.yanapaderina.rbot.accounts.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

// MVP-01
record NewAccount(@NotBlank String email, @NotBlank String password, @NotEmpty Set<String> roles) {
}
