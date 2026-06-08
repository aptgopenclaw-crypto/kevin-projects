package com.taipei.iot.auth.mapper;

import com.taipei.iot.auth.dto.response.UserInfoDto;
import com.taipei.iot.auth.entity.UserEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-03T13:55:34+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserInfoDto toUserInfoDto(UserEntity user, String tenantId, String tenantName, List<String> roles, String deptId, String deptName, List<String> permissions) {
        if ( user == null && tenantId == null && tenantName == null && roles == null && deptId == null && deptName == null && permissions == null ) {
            return null;
        }

        UserInfoDto.UserInfoDtoBuilder userInfoDto = UserInfoDto.builder();

        if ( user != null ) {
            userInfoDto.userId( user.getUserId() );
            userInfoDto.email( user.getEmail() );
            userInfoDto.displayName( user.getDisplayName() );
            if ( user.getIsSuperAdmin() != null ) {
                userInfoDto.isSuperAdmin( user.getIsSuperAdmin() );
            }
        }
        userInfoDto.tenantId( tenantId );
        userInfoDto.tenantName( tenantName );
        List<String> list = roles;
        if ( list != null ) {
            userInfoDto.roles( new ArrayList<String>( list ) );
        }
        userInfoDto.deptId( deptId );
        userInfoDto.deptName( deptName );
        List<String> list1 = permissions;
        if ( list1 != null ) {
            userInfoDto.permissions( new ArrayList<String>( list1 ) );
        }

        return userInfoDto.build();
    }
}
