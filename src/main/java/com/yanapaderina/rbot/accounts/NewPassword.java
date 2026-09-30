package com.yanapaderina.rbot.accounts;

import jakarta.validation.constraints.NotBlank;

// MVP-01
record NewPassword(@NotBlank String password) {
}
