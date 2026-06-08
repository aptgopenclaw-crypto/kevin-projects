package com.taipei.iot.auth.provider;

import com.taipei.iot.auth.provider.config.entity.TenantAuthConfigEntity;
import com.taipei.iot.auth.provider.config.repository.TenantAuthConfigRepository;
import com.taipei.iot.auth.provider.crypto.AuthConfigEncryptor;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes authentication requests to the appropriate {@link AuthenticationProvider} based
 * on the tenant's configured auth type.
 */
@Component
@Slf4j
public class AuthenticationDispatcher {

	private final Map<AuthType, AuthenticationProvider> providerMap;

	private final TenantAuthConfigRepository configRepository;

	private final AuthConfigEncryptor encryptor;

	public AuthenticationDispatcher(List<AuthenticationProvider> providers, TenantAuthConfigRepository configRepository,
			AuthConfigEncryptor encryptor) {
		this.providerMap = providers.stream()
			.collect(Collectors.toMap(AuthenticationProvider::getType, Function.identity()));
		this.configRepository = configRepository;
		this.encryptor = encryptor;
		log.info("AuthenticationDispatcher initialized with providers: {}", providerMap.keySet());
	}

	/**
	 * Dispatch an authentication request to the correct provider.
	 * @param request the authentication request
	 * @return authentication result from the matched provider
	 */
	public AuthenticationResult dispatch(AuthenticationRequest request) {
		// 1. Resolve tenant auth config (if tenantId provided)
		TenantAuthConfigEntity config = resolveConfig(request.getTenantId());

		// 2. Determine auth type (default LOCAL if no config or disabled)
		AuthType authType = determineAuthType(config);

		// 3. Find matching provider
		AuthenticationProvider provider = providerMap.get(authType);
		if (provider == null) {
			log.error("No provider registered for auth type: {}", authType);
			throw new BusinessException(ErrorCode.AUTH_PROVIDER_NOT_SUPPORTED);
		}

		// 4. Decrypt config and delegate
		String decryptedConfig = decryptConfig(config);

		try {
			return provider.authenticate(request, decryptedConfig);
		}
		catch (BusinessException e) {
			// If external provider fails and fallback is enabled, try LOCAL
			if (authType != AuthType.LOCAL && config != null && Boolean.TRUE.equals(config.getFallbackLocal())) {
				log.warn("External auth ({}) failed for tenant {}, falling back to LOCAL", authType,
						request.getTenantId());
				AuthenticationProvider localProvider = providerMap.get(AuthType.LOCAL);
				if (localProvider != null) {
					return localProvider.authenticate(request, null);
				}
			}
			throw e;
		}
	}

	/**
	 * Test connection for a given auth type and config JSON.
	 */
	public boolean testConnection(AuthType authType, String configJson) {
		AuthenticationProvider provider = providerMap.get(authType);
		if (provider == null) {
			throw new BusinessException(ErrorCode.AUTH_PROVIDER_NOT_SUPPORTED);
		}
		return provider.testConnection(configJson);
	}

	private TenantAuthConfigEntity resolveConfig(String tenantId) {
		if (tenantId == null || tenantId.isBlank()) {
			return null;
		}
		return configRepository.findByTenantId(tenantId).orElse(null);
	}

	private AuthType determineAuthType(TenantAuthConfigEntity config) {
		if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
			return AuthType.LOCAL;
		}
		return config.getAuthType();
	}

	private String decryptConfig(TenantAuthConfigEntity config) {
		if (config == null || config.getConfigJson() == null || config.getConfigJson().isBlank()) {
			return null;
		}
		return encryptor.decrypt(config.getConfigJson());
	}

}
