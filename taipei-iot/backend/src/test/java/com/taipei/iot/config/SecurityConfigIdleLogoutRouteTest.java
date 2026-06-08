package com.taipei.iot.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N-9: SecurityConfig 對 /v1/auth/idle-logout 應明確列出 authenticated() 規則， 而非僅靠
 * anyRequest().authenticated() catch-all 隱式保護。
 */
class SecurityConfigIdleLogoutRouteTest {

	@Test
	void securityConfig_shouldExplicitlyListIdleLogoutRoute() throws IOException {
		Path path = Path.of("src/main/java/com/taipei/iot/config/SecurityConfig.java");
		String source = Files.readString(path);

		assertThat(source).as("SecurityConfig should explicitly declare /v1/auth/idle-logout matcher")
			.contains("/v1/auth/idle-logout");
	}

	@Test
	void securityConfig_idleLogoutShouldRequireAuthentication() throws IOException {
		Path path = Path.of("src/main/java/com/taipei/iot/config/SecurityConfig.java");
		String source = Files.readString(path);

		// Find the line with idle-logout and verify it's followed by .authenticated()
		int idx = source.indexOf("/v1/auth/idle-logout");
		assertThat(idx).isGreaterThan(0);

		// The same line should end with .authenticated()
		String lineWithRoute = source.substring(idx, source.indexOf("\n", idx));
		assertThat(lineWithRoute).contains(".authenticated()");
	}

	@Test
	void securityConfig_idleLogoutShouldBePostMethod() throws IOException {
		Path path = Path.of("src/main/java/com/taipei/iot/config/SecurityConfig.java");
		String source = Files.readString(path);

		// The matcher should specify POST method
		assertThat(source).contains("requestMatchers(HttpMethod.POST, \"/v1/auth/idle-logout\")");
	}

}
