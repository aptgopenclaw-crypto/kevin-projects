# Spring Project Semantic Map

## Configuration Properties
- `app.auth.config-secret-key`: ***REDACTED***
- `management.endpoints.web.exposure.include`: health,info
- `spring.flyway.enabled`: true
- `iot.no-signal-detection.interval-ms`: ${IOT_NO_SIGNAL_INTERVAL:300000}
- `auth.cookie.same-site`: Lax
- `spring.flyway.schemas`: [iot_workflowdb]
- `tenant.mode`: ${TENANT_MODE:multi}
- `management.tracing.enabled`: true
- `management.endpoint.shutdown.enabled`: false
- `springdoc.swagger-ui.enabled`: false
- `spring.data.redis.lettuce.pool.max-active`: ${REDIS_MAX_ACTIVE:20}
- `mqtt.client-id`: ${MQTT_CLIENT_ID:taipei-iot-server}
- `virus-scan.clamav.timeout`: ${CLAMAV_TIMEOUT:5000}
- `spring.jpa.hibernate.ddl-auto`: none
- `auth.cookie.domain`: 
- `spring.flyway.locations`: classpath:db/migration
- `spring.flyway.out-of-order`: true
- `file.storage.local.root-dir`: ${FILE_STORAGE_ROOT:./uploads}
- `user.password.require-digit`: ***REDACTED***
- `app.frontend-base-url`: ${FRONTEND_BASE_URL:http://localhost:5173}
- `announcement.attachments.allowed-extensions`: ${ANNOUNCEMENT_ATTACHMENT_EXTS:pdf}
- `mqtt.qos`: 1
- `captcha.skip-verification`: true
- `spring.application.name`: taipei-iot
- `app.security.lock.max-fail-count`: 5
- `jwt.secret`: ***REDACTED***
- `spring.profiles.active`: dev
- `jwt.temporary-token-expiration`: ***REDACTED***
- `spring.datasource.password`: ***REDACTED***
- `user.password.require-lowercase`: ***REDACTED***
- `spring.data.redis.port`: ${REDIS_PORT:6379}
- `jwt.refresh-token-expiration`: ***REDACTED***
- `auth.cookie.secure`: true
- `springdoc.api-docs.enabled`: false
- `spring.datasource.url`: jdbc:postgresql://localhost:5432/mydb?currentSchema=iot_workflowdb
- `spring.servlet.multipart.max-request-size`: ${FILE_MAX_REQUEST_SIZE:15MB}
- `captcha.turnstile.site-key`: ***REDACTED***
- `spring.data.redis.lettuce.shutdown-timeout`: 1s
- `iot.no-signal-detection.enabled`: ${IOT_NO_SIGNAL_ENABLED:true}
- `management.endpoint.env.enabled`: false
- `file.validation.max-media-size`: ${FILE_MAX_MEDIA_SIZE:104857600}
- `file.validation.max-document-size`: ${FILE_MAX_DOCUMENT_SIZE:20971520}
- `tenant.default-id`: ${TENANT_DEFAULT_ID:DEFAULT}
- `cors.allowed-origins`: ${CORS_ALLOWED_ORIGINS:}
- `spring.data.redis.lettuce.pool.min-idle`: ${REDIS_MIN_IDLE:5}
- `spring.data.redis.password`: ***REDACTED***
- `file.validation.max-image-size`: ${FILE_MAX_IMAGE_SIZE:5242880}
- `app.security.lock.lock-duration-minutes`: 10
- `virus-scan.clamav.host`: ${CLAMAV_HOST:localhost}
- `user.password.history-count`: ***REDACTED***
- `spring.datasource.hikari.maximum-pool-size`: 10
- `spring.data.redis.lettuce.pool.max-wait`: ${REDIS_MAX_WAIT:1000ms}
- `captcha.ttl`: 300
- `mqtt.password`: ***REDACTED***
- `jwt.access-token-expiration`: ***REDACTED***
- `mqtt.telemetry-topic`: device/+/telemetry
- `mqtt.command-topic-prefix`: device/
- `spring.data.redis.timeout`: ${REDIS_TIMEOUT:2000ms}
- `server.port`: 8080
- `user.password.require-uppercase`: ***REDACTED***
- `mqtt.completion-timeout`: 30000
- `spring.data.redis.host`: ${REDIS_HOST:localhost}
- `auth.cookie.path`: /
- `user.password.max-age-days`: ***REDACTED***
- `spring.flyway.default-schema`: iot_workflowdb
- `user.password.min-length`: ***REDACTED***
- `spring.servlet.multipart.max-file-size`: ${FILE_MAX_SIZE:10MB}
- `captcha.length`: 4
- `virus-scan.clamav.port`: ${CLAMAV_PORT:3310}
- `virus-scan.enabled`: ${VIRUS_SCAN_ENABLED:false}
- `mqtt.username`: ${MQTT_USERNAME:}
- `spring.datasource.username`: postgres
- `management.endpoint.health.probes.enabled`: false
- `management.endpoint.health.show-details`: never
- `mqtt.broker-url`: ${MQTT_BROKER_URL:tcp://localhost:1883}
- `spring.jpa.properties.hibernate.default_schema`: iot_workflowdb
- `virus-scan.clamav.stream-max-length`: ${CLAMAV_STREAM_MAX_LENGTH:26214400}
- `auth.cookie.max-age`: 604800
- `app.security.scope-enforcement.mode`: ${SCOPE_ENFORCEMENT_MODE:enforce}
- `spring.data.redis.lettuce.pool.max-idle`: ${REDIS_MAX_IDLE:10}
- `management.tracing.sampling.probability`: 1.0
- `spring.datasource.hikari.schema`: iotdb
- `iot.device-token.header-name`: ***REDACTED***
- `captcha.turnstile.secret-key`: ***REDACTED***

## Data Entities
- **WorkflowRejectRequest** (Table: `(Record / DTO)`)
  - Long instanceId
  - String targetStepId
  - String comment
- **StepSlaDto** (Table: `(Record / DTO)`)
  - String stepId
  - String stepName
  - String assigneeUserId
  - WorkflowAction action
  - Integer slaDays
  - Double actualDays
  - boolean overdue
  - LocalDateTime enteredAt
  - LocalDateTime completedAt
- **WorkflowSlaDto** (Table: `(Record / DTO)`)
  - Long instanceId
  - String businessId
  - String businessType
  - WorkflowStatus status
  - LocalDateTime createdAt
  - LocalDateTime completedAt
  - Integer slaTotalDays
  - Double actualDays
  - boolean overdue
  - List<StepSlaDto> steps
- **WorkflowCancelRequest** (Table: `(Record / DTO)`)
  - Long instanceId
  - String comment
- **DelegateSetRequest** (Table: `(Record / DTO)`)
  - String delegateTo
  - String businessType
  - LocalDate effectiveFrom
  - LocalDate effectiveTo
- **WorkflowApproveRequest** (Table: `(Record / DTO)`)
  - Long instanceId
  - String comment
- **WorkflowStartRequest** (Table: `(Record / DTO)`)
  - String workflowCode
  - String businessId
  - String businessType
  - String departmentId
  - String district
  - String contractId
- **WorkflowResubmitRequest** (Table: `(Record / DTO)`)
  - Long instanceId
  - String comment
- **WorkflowInstanceEntity** (Table: `workflow_instances`)
  - Long id
  - String tenantId
  - Long workflowDefId
  - String businessId
  - String businessType
  - String currentStepId
  - WorkflowStatus status
  - String contextJson
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
  - LocalDateTime completedAt
- **WorkflowStepLogEntity** (Table: `workflow_step_logs`)
  - Long id
  - String tenantId
  - Long workflowInstanceId
  - String stepId
  - String stepName
  - String assigneeUserId
  - WorkflowAction action
  - String comment
  - String targetStepId
  - LocalDateTime enteredAt
  - LocalDateTime completedAt
- **DelegateSettingEntity** (Table: `delegate_settings`)
  - Long id
  - String tenantId
  - String delegateFor
  - String delegateTo
  - String businessType
  - LocalDate effectiveFrom
  - LocalDate effectiveTo
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
- **WorkflowDefinitionEntity** (Table: `workflow_definitions`)
  - Long id
  - String tenantId
  - String code
  - Integer version
  - String name
  - String stepsJson
  - Boolean enabled
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
- **ChangeEvent** (Table: `(Record / DTO)`)
  - String podId
  - String tenantId
  - boolean enabled
- **TenantEntity** (Table: `tenant`)
  - String tenantId
  - String tenantCode
  - String tenantName
  - String deploymentMode
  - Map<String,Object> config
  - Boolean enabled
  - LocalDateTime createTime
  - LocalDateTime updateTime
- **SystemSettingEntity** (Table: `system_settings`)
  - Long id
  - String tenantId
  - String settingKey
  - String settingValue
  - String description
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
  - Integer version
- **NotificationEntity** (Table: `notifications`)
  - Long id
  - String tenantId
  - String userId
  - NotificationType type
  - String title
  - String content
  - NotificationRefType refType
  - String refId
  - Boolean read
  - LocalDateTime readAt
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
  - LocalDateTime archivedAt
- **ResolvedTranslation** (Table: `(Record / DTO)`)
  - String title
  - String content
  - String lang
- **DownloadHandle** (Table: `(Record / DTO)`)
  - InputStream stream
  - String fileName
  - String mimeType
  - long size
- **AnnouncementAttachment** (Table: `announcement_attachments`)
  - Long id
  - String tenantId
  - Long announcementId
  - String fileName
  - Long fileSize
  - String mimeType
  - String filePath
  - String createdBy
  - LocalDateTime createdAt
- **AnnouncementRead** (Table: `announcement_reads`)
  - Long id
  - Long announcementId
  - String userId
  - LocalDateTime readAt
- **Announcement** (Table: `announcements`)
  - Long id
  - String tenantId
  - String title
  - String content
  - String contentText
  - String status
  - String scope
  - String category
  - Boolean pinned
  - Integer pinOrder
  - Boolean requiresAck
  - LocalDateTime publishAt
  - LocalDateTime expireAt
  - String createdBy
  - String createdByName
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
  - Long version
- **AnnouncementDept** (Table: `announcement_depts`)
  - Long announcementId
  - Long deptId
- **AnnouncementTranslation** (Table: `announcement_translations`)
  - Long id
  - Long announcementId
  - String langCode
  - String title
  - String content
  - String contentText
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
- **AuditRevisionEntity** (Table: `rev_info`)
  - String actionUserId
- **UserEventLogEntity** (Table: `user_event_log`)
  - Long userEventLogPk
  - String tenantId
  - String userId
  - String username
  - String userLabel
  - String email
  - String eventType
  - String eventDesc
  - String apiEndpoint
  - String payload
  - String errorCode
  - String message
  - String ipAddress
  - String userAgent
  - Long executionTime
  - Long deptId
  - String impersonatedBy
  - LocalDateTime createTime
- **UserContext** (Table: `(Record / DTO)`)
  - String username
  - String email
- **PasswordHistoryEntity** (Table: `password_history`)
  - Long id
  - String userId
  - String passwordHash
  - LocalDateTime createTime
- **UserInfoLogEntity** (Table: `user_info_log`)
  - Long id
  - String tenantId
  - String actionType
  - String actionUserId
  - String targetUserId
  - String email
  - String displayName
  - String roleCode
  - String deptId
  - String detail
  - String impersonatedBy
  - LocalDateTime createTime
- **ImpersonationSessionEntity** (Table: `impersonation_session`)
  - String STATUS_ACTIVE
  - String STATUS_REVOKED
  - String STATUS_EXPIRED
  - String id
  - String operatorUserId
  - String targetTenantId
  - String reason
  - String status
  - LocalDateTime startedAt
  - LocalDateTime expiresAt
  - LocalDateTime revokedAt
  - String revokedByUserId
  - LocalDateTime createTime
  - LocalDateTime updateTime
- **PlatformAnnouncement** (Table: `platform_announcements`)
  - Long id
  - String title
  - String content
  - String contentText
  - String status
  - String category
  - LocalDateTime publishAt
  - LocalDateTime expireAt
  - String createdBy
  - String createdByName
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
- **WorkflowStepLogDto** (Table: `(Record / DTO)`)
  - Long id
  - String stepId
  - String stepName
  - String assigneeUserId
  - String assigneeName
  - WorkflowAction action
  - String comment
  - String targetStepId
  - LocalDateTime enteredAt
  - LocalDateTime completedAt
- **AssetTransferRejectRequest** (Table: `(Record / DTO)`)
  - String comment
  - String targetStepId
- **AssetTransferResponse** (Table: `(Record / DTO)`)
  - Long id
  - String applicationNo
  - String applicantId
  - String applicantName
  - Long departmentId
  - String departmentName
  - String assetCode
  - String assetName
  - String transferType
  - Long targetDepartmentId
  - String reason
  - BigDecimal assetValue
  - AssetTransferStatus status
  - String currentAssignee
  - String currentAssigneeName
  - LocalDateTime createdAt
  - String createdBy
  - LocalDateTime updatedAt
  - LocalDateTime approvedAt
  - String approvedBy
  - String rejectReason
  - boolean canAct
- **RejectTargetDto** (Table: `(Record / DTO)`)
  - String stepId
  - String stepName
- **AssetTransferCreateRequest** (Table: `(Record / DTO)`)
  - String assetCode
  - String assetName
  - String transferType
  - Long departmentId
  - Long targetDepartmentId
  - String reason
  - BigDecimal assetValue
- **AssetTransferActionRequest** (Table: `(Record / DTO)`)
  - String comment
- **AssetTransferApplicationEntity** (Table: `asset_transfer_applications`)
  - Long id
  - String tenantId
  - String applicationNo
  - String applicantId
  - String applicantName
  - Long departmentId
  - String departmentName
  - String assetCode
  - String assetName
  - String transferType
  - Long targetDepartmentId
  - String reason
  - BigDecimal assetValue
  - Long workflowInstanceId
  - AssetTransferStatus status
  - String currentAssignee
  - LocalDateTime createdAt
  - String createdBy
  - LocalDateTime updatedAt
  - String updatedBy
  - LocalDateTime approvedAt
  - String approvedBy
  - String rejectReason
- **MenuEntity** (Table: `menus`)
  - Long menuId
  - Long parentId
  - String name
  - String menuType
  - String routeName
  - String routePath
  - String component
  - String permissionCode
  - String icon
  - Integer sortOrder
  - Boolean visible
  - Boolean keepAlive
  - String redirect
  - String scope
  - LocalDateTime createTime
  - LocalDateTime updateTime
- **RolePermissionEntity** (Table: `role_permissions`)
  - String roleId
  - String permissionId
  - String tenantId
- **PermissionEntity** (Table: `permissions`)
  - String permissionId
  - String code
  - String name
  - String groupName
  - Integer sortOrder
- **TenantAuthConfigEntity** (Table: `tenant_auth_config`)
  - Long id
  - String tenantId
  - AuthType authType
  - Boolean enabled
  - String configJson
  - Boolean fallbackLocal
  - LocalDateTime createdAt
  - LocalDateTime updatedAt
- **UserEntity** (Table: `users`)
  - String userId
  - String email
  - String passwordHash
  - String displayName
  - String phone
  - Boolean enabled
  - Boolean locked
  - LocalDateTime lockedAt
  - Integer loginFailCount
  - Boolean isSuperAdmin
  - LocalDateTime lastLoginAt
  - Boolean deleted
  - LocalDateTime deletedAt
  - LocalDateTime createTime
  - LocalDateTime updateTime
  - Boolean notifyEmailFlag
  - Boolean notifySmsFlag
  - LocalDateTime passwordChangedAt
  - Boolean forceChangePassword
  - AuthType authType
  - String externalId
- **RoleEntity** (Table: `roles`)
  - String roleId
  - String code
  - String name
  - String description
  - Boolean builtIn
  - Boolean enabled
  - String dataScope
  - LocalDateTime createTime
  - LocalDateTime updateTime
- **UserResetPasswordTokenEntity** (Table: `user_reset_password_token`)
  - String tokenId
  - String userId
  - String tokenHash
  - LocalDateTime expiredAt
  - Boolean used
  - LocalDateTime createTime
- **UserSessionEntity** (Table: `user_session`)
  - String sessionId
  - String userId
  - String tenantId
  - String ipAddress
  - String userAgent
  - LocalDateTime issuedAt
  - LocalDateTime lastSeenAt
  - LocalDateTime expiresAt
  - Boolean revoked
  - LocalDateTime revokedAt
- **ChangePasswordLogEntity** (Table: `change_password_log`)
  - Long id
  - String userId
  - String changeType
  - String ipAddress
  - LocalDateTime createTime
- **UserTenantMappingEntity** (Table: `user_tenant_mapping`)
  - Long id
  - String userId
  - String tenantId
  - String roleId
  - Long deptId
  - String defaultProjectId
  - Boolean enabled
  - UserEntity user
  - TenantEntity tenant
  - RoleEntity role
  - LocalDateTime createTime
  - LocalDateTime updateTime
- **SanitizeResult** (Table: `(Record / DTO)`)
  - byte[] bytes
  - int originalFrames
  - boolean wasDowngraded
  - int framesDropped
- **UploadRequest** (Table: `(Record / DTO)`)
  - MultipartFile file
  - String subDir
  - Set<String> additionalAllowedExtensions
  - boolean scanAfterStore
- **Result** (Table: `(Record / DTO)`)
  - String relativePath
  - String originalFileName
  - long size
  - String mimeType
- **VirusScanAuditEvent** (Table: `(Record / DTO)`)
  - Result result
  - String relativePath
  - String originalFileName
  - long size
  - String subDir
- **DeptInfoEntity** (Table: `dept_info`)
  - Long deptId
  - String tenantId
  - Long pid
  - String deptName
  - Integer deptSort
  - Short status
  - String hierarchyPath
  - String createBy
  - String updateBy
  - LocalDateTime createTime
  - LocalDateTime updateTime
- **TestTenantAwareEntity** (Table: `test_tenant_item`)
  - Long id
  - String tenantId
  - String name

## JPA Repositories
- WorkflowInstanceRepository {
      Optional<WorkflowInstanceEntity> findByIdForUpdate(Long id);
  }
- WorkflowDefinitionRepository {
      Optional<WorkflowDefinitionEntity> findLatestEnabledByCode(String code);
  }
- WorkflowStepLogRepository {
      List<WorkflowStepLogEntity> findByWorkflowInstanceIdOrderByEnteredAtAsc(Long workflowInstanceId);
      List<WorkflowStepLogEntity> findByAssigneeUserIdAndCompletedAtIsNull(String assigneeUserId);
      List<WorkflowStepLogEntity> findByAssigneeUserIdInAndCompletedAtIsNull(List<String> assigneeUserIds);
      Optional<WorkflowStepLogEntity> findCurrentByInstanceId(Long instanceId);
  }
- DelegateSettingRepository {
      Optional<DelegateSettingEntity> findActiveDelegate(String tenantId, String delegateFor, String businessType, LocalDate today);
      List<String> findDelegatedUserIds(String tenantId, String delegateTo, String businessType, LocalDate today);
      List<DelegateSettingEntity> findByDelegateForOrderByCreatedAtDesc(String delegateFor);
  }
- TenantRepository {
      Optional<TenantEntity> findByTenantCode(String tenantCode);
      List<TenantEntity> findByEnabledTrue();
  }
- SystemSettingRepository {
      Optional<SystemSettingEntity> findBySettingKey(String settingKey);
      List<SystemSettingEntity> findByTenantId(String tenantId);
      Optional<SystemSettingEntity> findByTenantIdAndSettingKey(String tenantId, String settingKey);
  }
- NotificationRepository {
      Page<NotificationEntity> findByUserIdAndArchivedAtIsNullOrderByCreatedAtDesc(String userId, Pageable pageable);
      long countUnreadByUserId(String userId);
      int markAllReadByUserId(String userId);
      Page<NotificationEntity> findByUserIdAndTypeAndArchivedAtIsNullOrderByCreatedAtDesc(String userId, NotificationType type, Pageable pageable);
      int archiveOldReadNotifications(String tenantId, LocalDateTime cutoff, LocalDateTime now);
      int markReadAtomic(Long id, String userId);
  }
- AnnouncementReadRepository {
      Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, String userId);
      List<AnnouncementRead> findByAnnouncementIdInAndUserId(List<Long> announcementIds, String userId);
      void markAsRead(Long announcementId, String userId);
      void markAllAsRead(String userId, String tenantId, Long userDeptId);
  }
- AnnouncementDeptRepository {
      List<AnnouncementDept> findByAnnouncementId(Long announcementId);
      void deleteByAnnouncementId(Long announcementId);
      List<AnnouncementDept> findByAnnouncementIdIn(List<Long> announcementIds);
      boolean existsByAnnouncementIdAndDeptId(Long announcementId, Long deptId);
  }
- AnnouncementStatsRepository {
      long countAudienceAll(String tenantId);
      long countAudienceDept(String tenantId, Collection<Long> deptIds);
      long countReadAll(Long announcementId, String tenantId);
      long countReadDept(Long announcementId, String tenantId, Collection<Long> deptIds);
      Page<Object[]> findUnreadUsersAll(Long announcementId, String tenantId, String keyword, Pageable pageable);
      Page<Object[]> findUnreadUsersDept(Long announcementId, String tenantId, Collection<Long> deptIds, String keyword, Pageable pageable);
  }
- AnnouncementRepository {
      Page<Announcement> findVisibleAnnouncements(Long userDeptId, String category, LocalDateTime now, Pageable pageable);
      Page<Announcement> findAdminAnnouncements(String statusFilter, String category, String keyword, LocalDateTime now, Pageable pageable);
      Page<Announcement> findDeptAdminAnnouncements(String userId, Long userDeptId, String statusFilter, String category, String keyword, LocalDateTime now, Pageable pageable);
      long countUnread(Long userDeptId, String userId, LocalDateTime now);
      Integer findMaxPinOrder();
      List<Announcement> findPinnedForDeptAdmin(String userId, Long userDeptId);
      List<Announcement> findAllPinned();
  }
- AnnouncementTranslationRepository {
      List<AnnouncementTranslation> findByAnnouncementId(Long announcementId);
      List<AnnouncementTranslation> findByAnnouncementIdIn(Collection<Long> announcementIds);
      void deleteByAnnouncementId(Long announcementId);
  }
- AnnouncementAttachmentRepository {
      List<AnnouncementAttachment> findByAnnouncementIdOrderByIdAsc(Long announcementId);
      List<AnnouncementAttachment> findByAnnouncementIdInOrderByIdAsc(List<Long> announcementIds);
  }
- UserEventLogRepository {
      int deleteByCreateTimeBefore(LocalDateTime cutoff);
      int deleteByTenantIdAndCreateTimeBefore(String tenantId, LocalDateTime cutoff);
      int deleteByTenantIdNullAndCreateTimeBefore(LocalDateTime cutoff);
  }
- PasswordHistoryRepository {
      List<PasswordHistoryEntity> findByUserIdOrderByCreateTimeDesc(String userId, Pageable pageable);
  }
- UserInfoLogRepository {
      List<UserInfoLogEntity> findByTargetUserIdOrderByCreateTimeDesc(String targetUserId);
  }
- ImpersonationSessionRepository {
      List<ImpersonationSessionEntity> findByOperatorUserIdOrderByStartedAtDesc(String operatorUserId);
      List<ImpersonationSessionEntity> findByOperatorUserIdAndStatusOrderByStartedAtDesc(String operatorUserId, String status);
  }
- PlatformAnnouncementRepository {
      Page<PlatformAnnouncement> findAdminList(String statusFilter, String category, String keyword, LocalDateTime now, Pageable pageable);
      Page<PlatformAnnouncement> findPublished(String category, LocalDateTime now, Pageable pageable);
  }
- AssetTransferApplicationRepository {
      Optional<AssetTransferApplicationEntity> findByApplicationNo(String applicationNo);
      List<AssetTransferApplicationEntity> findByApplicantIdOrderByCreatedAtDesc(String applicantId);
      List<AssetTransferApplicationEntity> findByWorkflowInstanceIdIn(Collection<Long> workflowInstanceIds);
  }
- MenuRepository {
      List<MenuEntity> findByParentIdOrderBySortOrder(Long parentId);
      List<MenuEntity> findAllByOrderBySortOrder();
      List<MenuEntity> findByPermissionCodeInAndVisibleTrue(Collection<String> codes);
      List<MenuEntity> findByPermissionCodeIsNullAndVisibleTrue();
      List<MenuEntity> findByScopeAndVisibleTrue(String scope);
      List<MenuEntity> findByScopeInAndVisibleTrue(Collection<String> scopes);
      boolean existsByParentId(Long parentId);
  }
- RolePermissionRepository {
      List<RolePermissionEntity> findByRoleIdAndTenantScope(String roleId, String tenantId);
      List<RolePermissionEntity> findByRoleIdInAndTenantScope(Collection<String> roleIds, String tenantId);
      List<RolePermissionEntity> findByRoleId(String roleId);
      void deleteByRoleId(String roleId);
  }
- PermissionRepository {
      List<PermissionEntity> findAllByOrderByGroupNameAscSortOrderAsc();
      List<PermissionEntity> findByCodeIn(Collection<String> codes);
      List<String> findAllCodesOrderByCode();
      List<String> findCodesByRoleAndTenant(String roleId, String tenantId);
      List<String> findCodesByRoleIdsAndTenant(Collection<String> roleIds, String tenantId);
  }
- TenantAuthConfigRepository {
      Optional<TenantAuthConfigEntity> findByTenantId(String tenantId);
      void deleteByTenantId(String tenantId);
  }
- ChangePasswordLogRepository
- UserTenantMappingRepository {
      List<UserTenantMappingEntity> findByUserIdAndEnabledTrue(String userId);
      Optional<UserTenantMappingEntity> findByUserIdAndTenantId(String userId, String tenantId);
      List<UserTenantMappingEntity> findByTenantIdAndEnabledTrue(String tenantId);
      List<UserTenantMappingEntity> findByTenantIdAndDeptIdAndEnabledTrue(String tenantId, Long deptId);
      Page<UserTenantMappingEntity> findByTenantId(String tenantId, Pageable pageable);
      Page<UserTenantMappingEntity> findByTenantIdAndDeptId(String tenantId, Long deptId, Pageable pageable);
      Page<UserTenantMappingEntity> findByTenantIdAndDeptIdIn(String tenantId, List<Long> deptIds, Pageable pageable);
      List<UserTenantMappingEntity> findByUserId(String userId);
      List<UserTenantMappingEntity> findByUserIdAndTenantEnabled(String userId);
      void clearDeptIdByTenantIdAndDeptId(String tenantId, Long deptId);
      Page<UserTenantMappingEntity> findActiveByTenantId(String tenantId, String keyword, Pageable pageable);
      Page<UserTenantMappingEntity> findActiveByTenantIdAndDeptId(String tenantId, Long deptId, String keyword, Pageable pageable);
      Page<UserTenantMappingEntity> findActiveByTenantIdAndDeptIdIn(String tenantId, Collection<Long> deptIds, String keyword, Pageable pageable);
      Page<UserTenantMappingEntity> findAllActive(String keyword, Pageable pageable);
      List<UserTenantMappingEntity> findByTenantIdAndRoleIdAndEnabledTrue(String tenantId, String roleId);
      List<UserTenantMappingEntity> findByTenantIdAndRoleIdAndEnabledTrueOrderByUserIdAsc(String tenantId, String roleId);
      List<UserTenantMappingEntity> findByTenantIdAndDeptIdAndRoleIdAndEnabledTrue(String tenantId, Long deptId, String roleId);
      List<UserTenantMappingEntity> findByTenantIdAndDeptIdAndRoleIdAndEnabledTrueOrderByUserIdAsc(String tenantId, Long deptId, String roleId);
  }
- UserRepository {
      Optional<UserEntity> findByEmail(String email);
      boolean existsByEmail(String email);
  }
- UserSessionRepository {
      List<UserSessionEntity> findActiveByUserId(String userId, LocalDateTime now);
      Optional<UserSessionEntity> findBySessionIdAndUserId(String sessionId, String userId);
      int revokeById(String sessionId, LocalDateTime now);
      int touch(String sessionId, LocalDateTime now, String ip, String ua);
      int revokeAllByUserIdExcept(String userId, String excludeSessionId, LocalDateTime now);
      int revokeAllByUserId(String userId, LocalDateTime now);
  }
- UserResetPasswordTokenRepository {
      Optional<UserResetPasswordTokenEntity> findByTokenHash(String tokenHash);
      int markUsedIfValid(String hash, LocalDateTime now);
  }
- RoleRepository {
      boolean existsByCode(String code);
      Optional<RoleEntity> findByCode(String code);
      List<RoleEntity> findAllByOrderByBuiltInDescCodeAsc();
      List<RoleEntity> findAllByOrderByBuiltInDescCreateTimeDesc();
  }
- DeptInfoRepository {
      List<DeptInfoEntity> findAllByStatusOrderByDeptSortAsc(Short status);
      List<DeptInfoEntity> findByHierarchyPathStartingWith(String prefix);
      Optional<DeptInfoEntity> findByDeptId(Long deptId);
      List<DeptInfoEntity> findByDeptIdIn(Collection<Long> deptIds);
      List<DeptInfoEntity> findByPid(Long pid);
      boolean existsByPid(Long pid);
      boolean existsByTenantIdAndDeptNameAndPid(String tenantId, String deptName, Long pid);
      Optional<DeptInfoEntity> findByTenantIdAndDeptName(String tenantId, String deptName);
  }
- CompliantRepo
- NonCompliantRepo
- TestTenantAwareRepository

## REST Endpoints
- `POST /v1/api/poc/workflow/start`
  - Handler: `BaseResponse<WorkflowInstanceEntity> start(WorkflowStartRequest req)`
- `POST /v1/api/poc/workflow/approve`
  - Handler: `BaseResponse<WorkflowInstanceEntity> approve(WorkflowApproveRequest req)`
- `POST /v1/api/poc/workflow/reject`
  - Handler: `BaseResponse<WorkflowInstanceEntity> reject(WorkflowRejectRequest req)`
- `POST /v1/api/poc/workflow/resubmit`
  - Handler: `BaseResponse<WorkflowInstanceEntity> resubmit(WorkflowResubmitRequest req)`
- `POST /v1/api/poc/workflow/cancel`
  - Handler: `BaseResponse<WorkflowInstanceEntity> cancel(WorkflowCancelRequest req)`
- `GET /v1/api/poc/workflow/instance/{id}`
  - Handler: `BaseResponse<WorkflowInstanceEntity> getInstance(Long id)`
- `GET /v1/api/poc/workflow/history/{id}`
  - Handler: `BaseResponse<List<WorkflowStepLogEntity>> getHistory(Long id)`
- `GET /v1/api/poc/workflow/sla/{id}`
  - Handler: `BaseResponse<WorkflowSlaDto> getSla(Long id)`
- `POST /v1/api/poc/workflow/delegate`
  - Handler: `BaseResponse<DelegateSettingEntity> setDelegate(DelegateSetRequest req)`
- `GET /v1/api/poc/workflow/delegate/my`
  - Handler: `BaseResponse<List<DelegateSettingEntity>> myDelegates()`
- `GET /v1/platform/tenants`
  - Handler: `BaseResponse<List<TenantDto>> listTenants()`
- `POST /v1/platform/tenants`
  - Handler: `BaseResponse<TenantDto> createTenant(CreateTenantRequest request)`
- `PUT /v1/platform/tenants/{tenantId}`
  - Handler: `BaseResponse<TenantDto> updateTenant(String tenantId, UpdateTenantRequest request)`
- `PATCH /v1/platform/tenants/{tenantId}/enabled`
  - Handler: `BaseResponse<Void> toggleEnabled(String tenantId, boolean enabled)`
- `GET /v1/auth/system-settings`
  - Handler: `BaseResponse<List<SystemSettingDto>> listSettings()`
- `PUT /v1/auth/system-settings/{key}`
  - Handler: `BaseResponse<SystemSettingDto> updateSetting(String key, String value)`
- `GET /v1/auth/system-settings/idle-timeout`
  - Handler: `BaseResponse<Integer> getIdleTimeout()`
- `PUT /v1/auth/system-settings/idle-timeout`
  - Handler: `BaseResponse<Integer> updateIdleTimeout(int minutes)`
- `GET /v1/auth/notifications`
  - Handler: `BaseResponse<PageResponse<NotificationResponse>> list(PageQuery pageQuery)`
- `GET /v1/auth/notifications/todos`
  - Handler: `BaseResponse<PageResponse<NotificationResponse>> listTodos(PageQuery pageQuery)`
- `GET /v1/auth/notifications/unread-count`
  - Handler: `BaseResponse<UnreadCountResponse> unreadCount()`
- `PATCH /v1/auth/notifications/{id}/read`
  - Handler: `BaseResponse<Void> markRead(Long id)`
- `PATCH /v1/auth/notifications/read-all`
  - Handler: `BaseResponse<Void> markAllRead()`
- `GET /v1/auth/announcements`
  - Handler: `BaseResponse<PageResponse<AnnouncementResponse>> list(String category, PageQuery pageQuery, String lang, String acceptLanguage)`
- `GET /v1/auth/announcements/admin`
  - Handler: `BaseResponse<PageResponse<AnnouncementResponse>> listAdmin(String statusFilter, String category, String keyword, PageQuery pageQuery, String lang, String acceptLanguage)`
- `GET /v1/auth/announcements/{id}`
  - Handler: `BaseResponse<AnnouncementResponse> getById(Long id, String lang, String acceptLanguage)`
- `GET /v1/auth/announcements/unread-count`
  - Handler: `BaseResponse<UnreadCountResponse> getUnreadCount()`
- `POST /v1/auth/announcements/{id}/read`
  - Handler: `BaseResponse<Void> markAsRead(Long id)`
- `POST /v1/auth/announcements/read-all`
  - Handler: `BaseResponse<Void> markAllAsRead()`
- `GET /v1/auth/announcements/{id}/read-stats`
  - Handler: `BaseResponse<AnnouncementReadStatsResponse> getReadStats(Long id)`
- `GET /v1/auth/announcements/{id}/unread-users`
  - Handler: `BaseResponse<PageResponse<AnnouncementUnreadUserResponse>> getUnreadUsers(Long id, String keyword, PageQuery pageQuery)`
- `GET /v1/auth/announcements/pinned`
  - Handler: `BaseResponse<List<AnnouncementResponse>> listPinned()`
- `PUT /v1/auth/announcements/pin-order`
  - Handler: `BaseResponse<Void> reorderPins(AnnouncementPinOrderRequest request)`
- `POST /v1/auth/announcements`
  - Handler: `BaseResponse<AnnouncementResponse> create(AnnouncementRequest request)`
- `PUT /v1/auth/announcements/{id}`
  - Handler: `BaseResponse<AnnouncementResponse> update(Long id, AnnouncementRequest request)`
- `DELETE /v1/auth/announcements/{id}`
  - Handler: `BaseResponse<Void> delete(Long id)`
- `GET /v1/auth/announcements/attachments/config`
  - Handler: `BaseResponse<List<String>> getAttachmentConfig()`
- `GET /v1/auth/announcements/{id}/attachments`
  - Handler: `BaseResponse<List<AnnouncementAttachmentResponse>> listAttachments(Long id)`
- `POST /v1/auth/announcements/{id}/attachments`
  - Handler: `BaseResponse<AnnouncementAttachmentResponse> uploadAttachment(Long id, MultipartFile file)`
- `GET /v1/auth/announcements/{id}/attachments/{attachmentId}/download`
  - Handler: `ResponseEntity<InputStreamResource> downloadAttachment(Long id, Long attachmentId)`
- `DELETE /v1/auth/announcements/{id}/attachments/{attachmentId}`
  - Handler: `BaseResponse<Void> deleteAttachment(Long id, Long attachmentId)`
- `GET /v1/auth/audit/categories`
  - Handler: `BaseResponse<List<String>> getCategories()`
- `GET /v1/auth/audit/user/usage/history/export`
  - Handler: `void exportUsageHistory(AuditQueryRequest request, String format, HttpServletResponse response)`
- `GET /v1/auth/audit/user/usage/history`
  - Handler: `BaseResponse<PageResponse<UserEventLogDto>> getUserUsageHistory(AuditQueryRequest request, int pageSize, int page)`
- `GET /v1/auth/audit/user/login/my`
  - Handler: `BaseResponse<PageResponse<UserEventLogDto>> getMyLoginLog(String eventType, String startTimestamp, String endTimestamp, String sort, int pageSize, int page)`
- `GET /v1/auth/users`
  - Handler: `BaseResponse<PageResponse<UserListItemDto>> listUsers(PageQuery pageQuery, String keyword, Long deptId)`
- `GET /v1/auth/users/{userId}`
  - Handler: `BaseResponse<UserListItemDto> getUser(String userId)`
- `POST /v1/auth/users`
  - Handler: `BaseResponse<UserListItemDto> createUser(Authentication authentication, CreateUserRequest req)`
- `PUT /v1/auth/users/{userId}`
  - Handler: `BaseResponse<UserListItemDto> updateUser(Authentication authentication, String userId, UpdateUserRequest req)`
- `DELETE /v1/auth/users/{userId}`
  - Handler: `BaseResponse<Void> disableUser(Authentication authentication, String userId)`
- `PATCH /v1/auth/users/{userId}/soft-delete`
  - Handler: `BaseResponse<Void> softDeleteUser(Authentication authentication, String userId)`
- `GET /v1/auth/users/{userId}/tenant-roles`
  - Handler: `BaseResponse<List<UserTenantMappingDto>> getUserTenantMappings(String userId)`
- `POST /v1/auth/users/{userId}/tenant-roles`
  - Handler: `BaseResponse<UserTenantMappingDto> addTenantRole(Authentication authentication, String userId, AddTenantRoleRequest req)`
- `DELETE /v1/auth/users/{userId}/tenant-roles/{mappingId}`
  - Handler: `BaseResponse<Void> removeTenantRole(Authentication authentication, String userId, Long mappingId)`
- `PUT /v1/auth/user/my`
  - Handler: `BaseResponse<UserEntity> updateOwnProfile(Authentication authentication, UpdateOwnProfileRequest req)`
- `POST /v1/auth/user/change-password`
  - Handler: `BaseResponse<Void> changePassword(Authentication authentication, ChangePasswordRequest req, String refreshToken)`
- `GET /v1/platform/users/{userId}/tenant-roles`
  - Handler: `BaseResponse<List<UserTenantMappingDto>> list(String userId)`
- `POST /v1/platform/users/{userId}/tenant-roles`
  - Handler: `BaseResponse<UserTenantMappingDto> add(Authentication authentication, String userId, AddTenantRoleRequest req)`
- `DELETE /v1/platform/users/{userId}/tenant-roles/{mappingId}`
  - Handler: `BaseResponse<Void> remove(Authentication authentication, String userId, Long mappingId)`
- `POST /v1/platform/impersonations`
  - Handler: `BaseResponse<ImpersonationTokenResponse> create(CreateImpersonationRequest req)`
- `DELETE /v1/platform/impersonations/{sessionId}`
  - Handler: `BaseResponse<Void> revoke(String sessionId)`
- `GET /v1/platform/impersonations`
  - Handler: `BaseResponse<List<ImpersonationSessionDto>> list(String status)`
- `GET /v1/platform/announcements`
  - Handler: `BaseResponse<PageResponse<PlatformAnnouncementResponse>> list(String statusFilter, String category, String keyword, int page, int size)`
- `GET /v1/platform/announcements/{id}`
  - Handler: `BaseResponse<PlatformAnnouncementResponse> getById(Long id)`
- `POST /v1/platform/announcements`
  - Handler: `BaseResponse<PlatformAnnouncementResponse> create(PlatformAnnouncementRequest request)`
- `PUT /v1/platform/announcements/{id}`
  - Handler: `BaseResponse<PlatformAnnouncementResponse> update(Long id, PlatformAnnouncementRequest request)`
- `DELETE /v1/platform/announcements/{id}`
  - Handler: `BaseResponse<Void> delete(Long id)`
- `GET /v1/auth/platform-announcements`
  - Handler: `BaseResponse<PageResponse<PlatformAnnouncementResponse>> list(String category, int page, int size)`
- `POST /v1/auth/asset-transfer/create`
  - Handler: `BaseResponse<AssetTransferResponse> create(AssetTransferCreateRequest req)`
- `POST /v1/auth/asset-transfer/create-and-submit`
  - Handler: `BaseResponse<AssetTransferResponse> createAndSubmit(AssetTransferCreateRequest req)`
- `POST /v1/auth/asset-transfer/submit/{id}`
  - Handler: `BaseResponse<AssetTransferResponse> submit(Long id)`
- `POST /v1/auth/asset-transfer/approve/{id}`
  - Handler: `BaseResponse<AssetTransferResponse> approve(Long id, AssetTransferActionRequest req)`
- `POST /v1/auth/asset-transfer/reject/{id}`
  - Handler: `BaseResponse<AssetTransferResponse> reject(Long id, AssetTransferRejectRequest req)`
- `POST /v1/auth/asset-transfer/resubmit/{id}`
  - Handler: `BaseResponse<AssetTransferResponse> resubmit(Long id, AssetTransferActionRequest req)`
- `POST /v1/auth/asset-transfer/cancel/{id}`
  - Handler: `BaseResponse<AssetTransferResponse> cancel(Long id, AssetTransferActionRequest req)`
- `GET /v1/auth/asset-transfer/{id}/history`
  - Handler: `BaseResponse<List<WorkflowStepLogDto>> getHistory(Long id)`
- `GET /v1/auth/asset-transfer/{id}/sla`
  - Handler: `BaseResponse<WorkflowSlaDto> getSla(Long id)`
- `GET /v1/auth/asset-transfer/{id}`
  - Handler: `BaseResponse<AssetTransferResponse> getById(Long id)`
- `GET /v1/auth/asset-transfer/{id}/reject-targets`
  - Handler: `BaseResponse<List<RejectTargetDto>> getRejectTargets(Long id)`
- `GET /v1/auth/asset-transfer/my`
  - Handler: `BaseResponse<List<AssetTransferResponse>> myApplications()`
- `GET /v1/auth/asset-transfer/pending`
  - Handler: `BaseResponse<List<AssetTransferResponse>> pending()`
- `GET /v1/auth/roles`
  - Handler: `BaseResponse<List<RoleDto>> listRoles()`
- `GET /v1/auth/roles/assignable`
  - Handler: `BaseResponse<List<RoleDto>> listAssignableRoles()`
- `POST /v1/auth/roles`
  - Handler: `BaseResponse<RoleDto> createRole(CreateRoleRequest request)`
- `PUT /v1/auth/roles/{roleId}`
  - Handler: `BaseResponse<RoleDto> updateRole(String roleId, UpdateRoleRequest request)`
- `PATCH /v1/auth/roles/{roleId}/enabled`
  - Handler: `BaseResponse<Void> toggleEnabled(String roleId, boolean enabled)`
- `GET /v1/auth/roles/{roleId}/permissions`
  - Handler: `BaseResponse<RolePermissionListDto> getRolePermissions(String roleId)`
- `PUT /v1/auth/roles/{roleId}/permissions`
  - Handler: `BaseResponse<RolePermissionListDto> assignPermissions(String roleId, AssignRolePermissionsRequest request)`
- `GET /v1/auth/permissions`
  - Handler: `BaseResponse<List<PermissionDto>> listPermissions()`
- `GET /v1/auth/menus/tree`
  - Handler: `BaseResponse<List<MenuDto>> getMenuTree()`
- `GET /v1/auth/menus/my`
  - Handler: `BaseResponse<List<UserMenuDto>> getMyMenus(Authentication authentication)`
- `POST /v1/auth/menus`
  - Handler: `BaseResponse<MenuDto> createMenu(CreateMenuRequest request)`
- `PUT /v1/auth/menus`
  - Handler: `BaseResponse<MenuDto> updateMenu(UpdateMenuRequest request)`
- `DELETE /v1/auth/menus/{menuId}`
  - Handler: `BaseResponse<Void> deleteMenu(Long menuId)`
- `PATCH /v1/auth/menus/{menuId}/visible`
  - Handler: `BaseResponse<Void> toggleVisible(Long menuId, boolean visible)`
- `GET /v1/platform/tenants/{tenantId}/auth-config`
  - Handler: `BaseResponse<TenantAuthConfigResponse> get(String tenantId)`
- `PUT /v1/platform/tenants/{tenantId}/auth-config`
  - Handler: `BaseResponse<TenantAuthConfigResponse> createOrUpdate(String tenantId, TenantAuthConfigRequest request)`
- `DELETE /v1/platform/tenants/{tenantId}/auth-config`
  - Handler: `BaseResponse<Void> delete(String tenantId)`
- `POST /v1/platform/tenants/{tenantId}/auth-config/test-connection`
  - Handler: `BaseResponse<Boolean> testConnection(String tenantId, TenantAuthConfigRequest request)`
- `GET /v1/auth/tenant-auth-config`
  - Handler: `BaseResponse<TenantAuthConfigResponse> get()`
- `PUT /v1/auth/tenant-auth-config`
  - Handler: `BaseResponse<TenantAuthConfigResponse> createOrUpdate(TenantAuthConfigRequest request)`
- `DELETE /v1/auth/tenant-auth-config`
  - Handler: `BaseResponse<Void> delete()`
- `POST /v1/auth/tenant-auth-config/test-connection`
  - Handler: `BaseResponse<Boolean> testConnection(TenantAuthConfigRequest request)`
- `GET /v1/noauth/turnstile/config`
  - Handler: `BaseResponse<TurnstileConfigResponse> getTurnstileConfig()`
- `POST /v1/noauth/captcha`
  - Handler: `BaseResponse<CaptchaResponse> generateCaptcha()`
- `POST /v1/noauth/login`
  - Handler: `BaseResponse<LoginResult> login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)`
- `POST /v1/noauth/token/refresh`
  - Handler: `BaseResponse<TokenResult> refreshToken(String refreshToken, HttpServletRequest httpRequest, HttpServletResponse httpResponse)`
- `POST /v1/noauth/user/forgot-password`
  - Handler: `BaseResponse<Void> forgotPassword(ForgotPasswordRequest request)`
- `PUT /v1/noauth/user/reset-password`
  - Handler: `BaseResponse<Void> resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest)`
- `POST /v1/noauth/user/force-change-password`
  - Handler: `BaseResponse<LoginResult> forceChangePassword(ForceChangePasswordRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)`
- `POST /v1/auth/select-tenant`
  - Handler: `BaseResponse<TokenResult> selectTenant(SelectTenantRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)`
- `POST /v1/auth/switch-tenant`
  - Handler: `BaseResponse<TokenResult> switchTenant(SwitchTenantRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)`
- `POST /v1/auth/logout`
  - Handler: `BaseResponse<Void> logout(String refreshToken, HttpServletResponse httpResponse)`
- `POST /v1/auth/idle-logout`
  - Handler: `BaseResponse<Void> idleTimeoutLogout(String refreshToken, HttpServletResponse httpResponse)`
- `GET /v1/auth/user/info`
  - Handler: `BaseResponse<UserInfoDto> getUserInfo()`
- `GET /v1/auth/sessions`
  - Handler: `BaseResponse<List<SessionDto>> listMySessions(String refreshToken)`
- `DELETE /v1/auth/sessions/{sessionId}`
  - Handler: `BaseResponse<Void> revokeSession(String sessionId)`
- `GET /v1/noauth/password-policy/describe`
  - Handler: `BaseResponse<PasswordPolicyDto> describe(String tenantId)`
- `GET /v1/platform/password-policy`
  - Handler: `BaseResponse<Map<String,String>> getPlatformDefaults()`
- `PUT /v1/platform/password-policy`
  - Handler: `BaseResponse<Void> update(UpdatePasswordPolicyRequest req)`
- `GET /v1/auth/password-policy`
  - Handler: `BaseResponse<PasswordPolicyDto> getEffective()`
- `GET /v1/auth/password-policy/tenant`
  - Handler: `BaseResponse<Map<String,String>> getTenantOverrides()`
- `PUT /v1/auth/password-policy/tenant`
  - Handler: `BaseResponse<Void> updateTenantOverride(UpdatePasswordPolicyRequest req)`
- `DELETE /v1/auth/password-policy/tenant/{key}`
  - Handler: `BaseResponse<Void> deleteTenantOverride(String key)`
- `GET /v1/auth/dept/list`
  - Handler: `BaseResponse<List<DeptDto>> getDeptTree()`
- `GET /v1/auth/dept/options`
  - Handler: `BaseResponse<List<DeptOptionVO>> getDeptOptions()`
- `GET /v1/auth/dept/scope-options`
  - Handler: `BaseResponse<List<DeptOptionVO>> getScopedDeptOptions()`
- `GET /v1/auth/dept/{deptId}`
  - Handler: `BaseResponse<DeptDto> getDeptById(Long deptId)`
- `POST /v1/auth/dept`
  - Handler: `BaseResponse<DeptDto> createDept(CreateDeptRequest request)`
- `PUT /v1/auth/dept`
  - Handler: `BaseResponse<DeptDto> updateDept(UpdateDeptRequest request)`
- `DELETE /v1/auth/dept/{deptId}`
  - Handler: `BaseResponse<Void> deleteDept(Long deptId)`
- `GET /v1/noauth/csp-test`
  - Handler: `ResponseEntity<String> cspTest()`

## Spring Beans & Wiring
- **UserMapperImpl** (`@Component`)
  - Dependencies: None
- **WorkflowEngine** (`@Service`)
  - Dependencies: None
- **WorkflowSlaService** (`@Service`)
  - Dependencies: None
- **OrgAssigneeResolver** (`@Service`)
  - Dependencies: None
- **WorkflowNotificationListener** (`@Component`)
  - Dependencies: None
- **WorkflowPocController** (`@RestController`)
  - Dependencies: None
- **TenantConsistencyValidator** (`@Component`)
  - Dependencies: None
- **TenantEnabledCache** (`@Component`)
  - Dependencies:
    - TenantRepository
    - ObjectMapper
    - ObjectProvider<StringRedisTemplate>
    - ObjectProvider<RedisMessageListenerContainer>
- **TenantSystemContextAspect** (`@Component`)
  - Dependencies: None
- **TenantInterceptor** (`@Component`)
  - Dependencies: None
- **TenantAdminService** (`@Service`)
  - Dependencies: None
- **TenantFilterAspect** (`@Component`)
  - Dependencies: None
- **TenantAdminController** (`@RestController`)
  - Dependencies: None
- **SystemSettingService** (`@Service`)
  - Dependencies: None
- **SystemSettingController** (`@RestController`)
  - Dependencies: None
- **NotificationService** (`@Service`)
  - Dependencies: None
- **NotificationPurgeJob** (`@Component`)
  - Dependencies: None
- **NotificationController** (`@RestController`)
  - Dependencies: None
- **StompAuthInterceptor** (`@Configuration`)
  - Dependencies: None
- **WebSocketConfig** (`@Configuration`)
  - Dependencies: None
- **NoOpSmsChannel** (`@Component`)
  - Dependencies: None
- **EmailChannel** (`@Component`)
  - Dependencies: None
- **NoOpEmailChannel** (`@Component`)
  - Dependencies: None
- **InAppChannel** (`@Component`)
  - Dependencies: None
- **AnnouncementReadService** (`@Service`)
  - Dependencies: None
- **AnnouncementService** (`@Service`)
  - Dependencies: None
- **AnnouncementAttachmentService** (`@Service`)
  - Dependencies:
    - AnnouncementRepository
    - AnnouncementAttachmentRepository
    - AnnouncementDeptRepository
    - FileStorageService
    - FileValidationService
    - String
- **HtmlSanitizerService** (`@Service`)
  - Dependencies: None
- **AnnouncementController** (`@RestController`)
  - Dependencies: None
- **AuditAsyncWriter** (`@Service`)
  - Dependencies: None
- **AuditService** (`@Service`)
  - Dependencies: None
- **VirusScanAuditListener** (`@Component`)
  - Dependencies: None
- **AuditPurgeJob** (`@Component`)
  - Dependencies: None
- **AuditController** (`@RestController`)
  - Dependencies: None
- **BaseLoggerAspect** (`@Component`)
  - Dependencies: None
- **AuditAsyncConfig** (`@Configuration`)
  - Dependencies: None
- **UserAdminService** (`@Service`)
  - Dependencies: None
- **PasswordValidator** (`@Service`)
  - Dependencies: None
- **UserAuditService** (`@Service`)
  - Dependencies: None
- **UserSelfService** (`@Service`)
  - Dependencies: None
- **UserAdminController** (`@RestController`)
  - Dependencies: None
- **UserSelfController** (`@RestController`)
  - Dependencies: None
- **PlatformUserTenantMappingController** (`@RestController`)
  - Dependencies: None
- **ImpersonationService** (`@Service`)
  - Dependencies: None
- **PlatformImpersonationController** (`@RestController`)
  - Dependencies: None
- **PlatformAnnouncementService** (`@Service`)
  - Dependencies: None
- **PlatformAnnouncementController** (`@RestController`)
  - Dependencies: None
- **PlatformAnnouncementReadController** (`@RestController`)
  - Dependencies: None
- **AssetTransferService** (`@Service`)
  - Dependencies: None
- **AssetTransferController** (`@RestController`)
  - Dependencies: None
- **RoleService** (`@Service`)
  - Dependencies: None
- **PermissionService** (`@Service`)
  - Dependencies: None
- **MenuService** (`@Service`)
  - Dependencies: None
- **RoleController** (`@RestController`)
  - Dependencies: None
- **PermissionController** (`@RestController`)
  - Dependencies: None
- **MenuController** (`@RestController`)
  - Dependencies: None
- **PasswordExpiryChecker** (`@Component`)
  - Dependencies: None
- **PasswordPolicyDao** (`@Repository`)
  - Dependencies: None
- **PasswordPolicyService** (`@Service`)
  - Dependencies: None
- **PasswordPolicyResolver** (`@Component`)
  - Dependencies: None
- **PasswordResetMailService** (`@Service`)
  - Dependencies: None
- **NoOpCaptchaServiceImpl** (`@Service`)
  - Dependencies: None
- **AuthServiceImpl** (`@Service`)
  - Dependencies: None
- **UserSessionServiceImpl** (`@Service`)
  - Dependencies: None
- **TurnstileServiceImpl** (`@Service`)
  - Dependencies:
    - Builder
    - StringRedisTemplate
- **ResetPasswordTokenClaimer** (`@Component`)
  - Dependencies: None
- **CaptchaServiceImpl** (`@Service`)
  - Dependencies: None
- **AuthConfigEncryptor** (`@Component`)
  - Dependencies: None
- **LdapAuthProvider** (`@Component`)
  - Dependencies: None
- **LocalAuthProvider** (`@Component`)
  - Dependencies: None
- **AuthenticationDispatcher** (`@Component`)
  - Dependencies:
    - List<AuthenticationProvider>
    - TenantAuthConfigRepository
    - AuthConfigEncryptor
    - UserRepository
- **TenantAuthConfigServiceImpl** (`@Service`)
  - Dependencies: None
- **PlatformTenantAuthConfigController** (`@RestController`)
  - Dependencies: None
- **TenantAuthConfigController** (`@RestController`)
  - Dependencies: None
- **AuthController** (`@RestController`)
  - Dependencies: None
- **NoauthPasswordPolicyController** (`@RestController`)
  - Dependencies: None
- **PlatformPasswordPolicyController** (`@RestController`)
  - Dependencies: None
- **TenantPasswordPolicyController** (`@RestController`)
  - Dependencies: None
- **ScopeEnforcementFilter** (`@Component`)
  - Dependencies:
    - String
- **JwtAuthenticationFilter** (`@Component`)
  - Dependencies: None
- **JwtUtil** (`@Component`)
  - Dependencies:
    - String
    - long
- **DeprecatedApiInterceptor** (`@Component`)
  - Dependencies: None
- **RateLimitInterceptor** (`@Component`)
  - Dependencies: None
- **ClamAvHealthIndicator** (`@Component`)
  - Dependencies: None
- **LocalFileStorageService** (`@Service`)
  - Dependencies:
    - String
- **ImageSanitizer** (`@Component`)
  - Dependencies: None
- **ClamAvVirusScanService** (`@Service`)
  - Dependencies:
    - String
    - int
    - long
- **FileUploadTemplate** (`@Service`)
  - Dependencies: None
- **FileValidationService** (`@Service`)
  - Dependencies:
    - long
- **NoOpVirusScanService** (`@Service`)
  - Dependencies: None
- **DevProfileSecurityWarning** (`@Component`)
  - Dependencies: None
- **DataScopeHelper** (`@Component`)
  - Dependencies: None
- **DeptService** (`@Service`)
  - Dependencies: None
- **DeptController** (`@RestController`)
  - Dependencies: None
- **DataPermissionAspect** (`@Component`)
  - Dependencies: None
- **JacksonConfig** (`@Configuration`)
  - Dependencies: None
- **RedisConnectionValidator** (`@Component`)
  - Dependencies: None
- **RedisConfig** (`@Configuration`)
  - Dependencies: None
- **JpaAuditConfig** (`@Configuration`)
  - Dependencies: None
- **SecurityProfileValidator** (`@Component`)
  - Dependencies: None
- **SecurityConfig** (`@Configuration`)
  - Dependencies: None
- **OpenApiConfig** (`@Configuration`)
  - Dependencies: None
- **CorsProperties** (`@Component`)
  - Dependencies: None
- **WebMvcConfig** (`@Configuration`)
  - Dependencies: None
- **TracingConfig** (`@Configuration`)
  - Dependencies: None
- **TestConfig** (`@Configuration`)
  - Dependencies: None
- **TestConfig** (`@Configuration`)
  - Dependencies: None
- **NoopController** (`@RestController`)
  - Dependencies: None

