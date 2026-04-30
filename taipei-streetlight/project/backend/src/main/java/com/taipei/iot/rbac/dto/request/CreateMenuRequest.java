package com.taipei.iot.rbac.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateMenuRequest {
    private Long parentId;

    @NotBlank
    private String name;

    @NotBlank
    private String menuType;

    private String routeName;
    private String routePath;
    private String component;
    private String permissionCode;
    private String icon;
    private int sortOrder;
    private boolean visible;
    private boolean keepAlive;
    private String redirect;
}
