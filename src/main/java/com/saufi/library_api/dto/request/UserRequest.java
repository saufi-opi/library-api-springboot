package com.saufi.library_api.dto.request;

import com.saufi.library_api.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @ValidPassword
    private String password;

    private String fullName;
}
