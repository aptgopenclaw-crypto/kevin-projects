package com.taipei.iot.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String displayName;

	private String phone;

	@NotBlank
	@Size(min = 8)
	private String initialPassword;

	private String tenantId;

	@NotBlank
	private String roleId;

	private Long deptId;

}
