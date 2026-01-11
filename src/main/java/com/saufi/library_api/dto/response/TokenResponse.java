package com.saufi.library_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "bearer";

    private Long expiresIn; // seconds until expiration
}
