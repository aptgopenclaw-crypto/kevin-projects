package com.taipei.iot.dept.aspect;

import com.taipei.iot.common.dto.UserInfo;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.annotation.DataPermission;
import com.taipei.iot.dept.context.DataScopeContext;
import com.taipei.iot.dept.context.DataScopeFilter;
import com.taipei.iot.dept.entity.DeptInfoEntity;
import com.taipei.iot.dept.enums.DataScopeEnum;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DataPermissionAspect {

    private final DeptInfoRepository deptInfoRepository;

    @Around("@annotation(dataPermission)")
    public Object enforce(ProceedingJoinPoint pjp, DataPermission dataPermission) throws Throwable {
        UserInfo user = SecurityContextUtils.getUserInfo();

        if (user == null || user.getDataScope() == null) {
            // Strict: no user info → inject impossible condition → empty result
            DataScopeContext.set(DataScopeFilter.exact(dataPermission.deptIdField(), -1L));
            try {
                return pjp.proceed();
            } finally {
                DataScopeContext.clear();
            }
        }

        DataScopeEnum scope = DataScopeEnum.fromString(user.getDataScope());
        Long deptId = user.getDeptId();

        switch (scope) {
            case ALL:
                // No restriction
                break;

            case THIS_LEVEL:
                if (deptId == null) {
                    // Strict: deptId null → empty result (D4 Option B)
                    DataScopeContext.set(DataScopeFilter.exact(dataPermission.deptIdField(), -1L));
                } else {
                    DataScopeContext.set(DataScopeFilter.exact(dataPermission.deptIdField(), deptId));
                }
                break;

            case THIS_LEVEL_AND_BELOW:
                if (deptId == null) {
                    // Strict: deptId null → empty result (D4 Option B)
                    DataScopeContext.set(DataScopeFilter.exact(dataPermission.deptIdField(), -1L));
                } else {
                    String hierarchyPath = deptInfoRepository.findById(deptId)
                            .map(DeptInfoEntity::getHierarchyPath)
                            .orElse(null);

                    if (hierarchyPath != null) {
                        DataScopeContext.set(DataScopeFilter.hierarchyPrefix(
                                dataPermission.hierarchyPathField(), hierarchyPath));
                    } else {
                        // hierarchy_path not found → strict empty result (D4 Option B)
                        DataScopeContext.set(DataScopeFilter.exact(dataPermission.deptIdField(), -1L));
                    }
                }
                break;
        }

        try {
            return pjp.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
