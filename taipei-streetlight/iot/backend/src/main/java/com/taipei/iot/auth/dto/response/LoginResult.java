package com.taipei.iot.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {
    private String accessToken;
    private String refreshToken;
    private boolean needsSelection;
    private List<TenantOption> tenants;
}
