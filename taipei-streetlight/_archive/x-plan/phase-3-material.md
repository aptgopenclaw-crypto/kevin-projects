# Phase 3：材料管理 — 07 庫存管理全套

> Flyway V40 ~ V41 ｜涵蓋模組：07 材料管理
> 前置條件：Phase 2 完成（repair_tickets + ticket_attachments + workflow 事件串接）
> 後續依賴：Phase 4（換裝維護）依賴本 Phase 的 material_specs + inventory + approved_materials
>
> **注意事項**：
> - V35~V39 已被 Phase 1/2 佔用，本 Phase 從 V40 起編
> - API 路徑統一使用 `/v1/auth/` 前綴（與既有慣例一致）
> - `issue_requests.replacement_order_id` 為 nullable（Phase 4 才有 replacement_orders 表）
> - `TenantContext.getCurrentTenantId()`（不是 `getTenantId()`）
> - Controller 模式：`@RestController` + `@RequestMapping` + `@RequiredArgsConstructor`，不用 `@Tag`
> - 分頁模式：`@RequestParam(defaultValue="0") int page, int size` → `PageRequest.of(page, size)`
> - 回傳模式：`BaseResponse<PageResponse<T>>`，用 `toPageResponse()` helper 轉換
> - Entity 不用 MapStruct，用手動 Builder 模式

---

## 總覽

| Step | Flyway | 範圍 | 產出 |
|------|--------|------|------|
| 6 | V40 | 07 warehouses + material_specs + suppliers + inventory + approved_materials | 5 張表 + indexes |
| 7 | V41 | 07 purchase_orders + purchase_items + receiving_records + issue_requests + issue_records + disposal_records + inventory_adjustments + menu + permission | 7 張表 + 選單 + 權限 + role binding |

---

## Step 6：V40 — 庫別 + 材料規格 + 廠商 + 庫存 + 合格材料

### 6-1 Flyway Migration

**檔案**：`V40__material__create_base_tables.sql`

**建表順序**：
1. `warehouses`（無 FK 依賴其他新表）
2. `material_specs`（無 FK 依賴其他新表）
3. `suppliers`（無 FK 依賴其他新表）
4. `inventory`（FK → warehouses, material_specs）
5. `approved_materials`（FK → contracts；`material_spec_id` FK → material_specs）

> DDL 已在 [cross-module §2-3 / 07 區塊](cross-module-03-07-unified-design.md) 完整定義。

**DDL 摘要**：

```sql
-- 1. 庫別
CREATE TABLE warehouses (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    warehouse_code  VARCHAR(50)     NOT NULL,
    warehouse_name  VARCHAR(200)    NOT NULL,
    location        VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, warehouse_code)
);

-- 2. 材料規格
CREATE TABLE material_specs (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    spec_code       VARCHAR(100)    NOT NULL,
    spec_name       VARCHAR(300)    NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    unit            VARCHAR(20)     NOT NULL DEFAULT 'PCS',
    attributes      JSONB           DEFAULT '{}',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, spec_code)
);

-- 3. 廠商
CREATE TABLE suppliers (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    supplier_code   VARCHAR(100)    NOT NULL,
    supplier_name   VARCHAR(300)    NOT NULL,
    contact_name    VARCHAR(100),
    contact_phone   VARCHAR(50),
    contact_email   VARCHAR(200),
    address         TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, supplier_code)
);

-- 4. 庫存
CREATE TABLE inventory (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    warehouse_id        BIGINT          NOT NULL REFERENCES warehouses(id),
    material_spec_id    BIGINT          NOT NULL REFERENCES material_specs(id),
    quantity_on_hand    INT             NOT NULL DEFAULT 0,
    safety_stock        INT             NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, warehouse_id, material_spec_id)
);

-- 5. 審驗合格材料（從原 Phase 3 換裝模組移入，屬於材料管理範疇）
CREATE TABLE approved_materials (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    material_spec_id    BIGINT          NOT NULL REFERENCES material_specs(id),
    contract_id         BIGINT          REFERENCES contracts(id),
    material_number     VARCHAR(100)    NOT NULL,
    approval_date       DATE            NOT NULL,
    batch_number        VARCHAR(100),
    brand               VARCHAR(200),
    model               VARCHAR(200),
    spec_details        JSONB           DEFAULT '{}',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, material_number)
);
```

### 6-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| **Entities** | | | |
| 1 | Entity | `Warehouse.java` | `TenantAware`, warehouse_code unique |
| 2 | Entity | `WarehouseStatus.java` (enum) | ACTIVE, INACTIVE |
| 3 | Entity | `MaterialSpec.java` | `TenantAware`, spec_code unique, JSONB attributes |
| 4 | Entity | `MaterialCategory.java` (enum) | LUMINAIRE, CONTROLLER, POLE, POLE_NUMBER, CABLE, OTHER |
| 5 | Entity | `MaterialStatus.java` (enum) | ACTIVE, DEPRECATED |
| 6 | Entity | `Supplier.java` | `TenantAware`, supplier_code unique |
| 7 | Entity | `SupplierStatus.java` (enum) | ACTIVE, INACTIVE |
| 8 | Entity | `Inventory.java` | `TenantAware`, UNIQUE(tenant_id, warehouse_id, material_spec_id) |
| 9 | Entity | `ApprovedMaterial.java` | `TenantAware`, FK → MaterialSpec, Contract |
| 10 | Entity | `ApprovedMaterialStatus.java` (enum) | ACTIVE, EXPIRED, REVOKED |
| **Repositories** | | | |
| 11 | Repository | `WarehouseRepository.java` | `TenantScopedRepository` |
| 12 | Repository | `MaterialSpecRepository.java` | `TenantScopedRepository` |
| 13 | Repository | `SupplierRepository.java` | `TenantScopedRepository` |
| 14 | Repository | `InventoryRepository.java` | `TenantScopedRepository` |
| 15 | Repository | `ApprovedMaterialRepository.java` | `TenantScopedRepository` |
| **DTOs** | | | |
| 16 | DTO | `WarehouseRequest.java` / `WarehouseResponse.java` | |
| 17 | DTO | `MaterialSpecRequest.java` / `MaterialSpecResponse.java` | 含 JSONB attributes |
| 18 | DTO | `SupplierRequest.java` / `SupplierResponse.java` | |
| 19 | DTO | `InventoryResponse.java` | 含 warehouseName, materialSpecName, safetyStockAlert |
| 20 | DTO | `InventoryQueryParams.java` | warehouseId, category, keyword, belowSafetyStock |
| 21 | DTO | `InventorySummaryResponse.java` | 按材料類別彙總統計 |
| 22 | DTO | `ApprovedMaterialRequest.java` / `ApprovedMaterialResponse.java` | |
| 23 | DTO | `ApprovedMaterialImportRequest.java` | 批次匯入（06-4 需求） |
| **Services** | | | |
| 24 | Service | `WarehouseService.java` | CRUD |
| 25 | Service | `MaterialSpecService.java` | CRUD |
| 26 | Service | `SupplierService.java` | CRUD |
| 27 | Service | `InventoryService.java` | 庫存查詢 + 安全庫存預警 + 統計 |
| 28 | Service | `ApprovedMaterialService.java` | CRUD + 批次匯入 + 狀態管理 |
| **Controllers** | | | |
| 29 | Controller | `WarehouseController.java` | CRUD |
| 30 | Controller | `MaterialSpecController.java` | CRUD |
| 31 | Controller | `SupplierController.java` | CRUD |
| 32 | Controller | `InventoryController.java` | 庫存查詢+統計 |
| 33 | Controller | `ApprovedMaterialController.java` | CRUD + 批次匯入 |

### 6-3 庫存安全量預警（E12）

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查詢低於安全庫存的品項
     */
    public List<InventoryResponse> findBelowSafetyStock() {
        return inventoryRepo.findByQuantityOnHandLessThanSafetyStock(
            TenantContext.getCurrentTenantId());
    }

    /**
     * 定時排程：每日 08:00 檢查安全庫存
     * 低於安全量 → 推播通知（E12）
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkSafetyStock() {
        TenantContext.setSystemContext();
        try {
            List<Inventory> alerts = inventoryRepo
                .findBelowSafetyStockAllTenants();

            for (Inventory inv : alerts) {
                eventPublisher.publishEvent(new LowStockAlertEvent(
                    inv.getTenantId(),
                    inv.getMaterialSpec().getSpecName(),
                    inv.getWarehouse().getWarehouseName(),
                    inv.getQuantityOnHand(),
                    inv.getSafetyStock()
                ));
            }
        } finally {
            TenantContext.clear();
        }
    }
}
```

### 6-4 合格材料批次匯入（06-4）

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovedMaterialService {

    private final ApprovedMaterialRepository repo;
    private final MaterialSpecRepository materialSpecRepo;
    private final ContractRepository contractRepo;

    /**
     * 06-4 審驗合格材料批次匯入
     * 支援 CSV / Excel 格式
     */
    @Transactional
    @AuditEvent(AuditEventType.IMPORT_APPROVED_MATERIAL)
    public ImportResult batchImport(MultipartFile file) {
        List<ApprovedMaterialImportRow> rows = parseFile(file);

        int success = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            try {
                if (repo.existsByTenantIdAndMaterialNumber(
                        TenantContext.getCurrentTenantId(), row.getMaterialNumber())) {
                    skipped++;
                    continue;
                }

                ApprovedMaterial material = ApprovedMaterial.builder()
                    .materialSpecId(resolveMaterialSpecId(row.getSpecCode()))
                    .contractId(resolveContractId(row.getContractCode()))
                    .materialNumber(row.getMaterialNumber())
                    .approvalDate(row.getApprovalDate())
                    .batchNumber(row.getBatchNumber())
                    .brand(row.getBrand())
                    .model(row.getModel())
                    .specDetails(row.getSpecDetails())
                    .status(ApprovedMaterialStatus.ACTIVE)
                    .build();
                repo.save(material);
                success++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
            }
        }

        return new ImportResult(success, skipped, errors);
    }
}
```

### 6-5 API 端點（基礎表）

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| **庫別** | | | |
| GET | `/v1/auth/material/warehouses` | MATERIAL_VIEW | 庫別列表 |
| POST | `/v1/auth/material/warehouses` | MATERIAL_MANAGE | 新增庫別 |
| PUT | `/v1/auth/material/warehouses/{id}` | MATERIAL_MANAGE | 編輯 |
| DELETE | `/v1/auth/material/warehouses/{id}` | MATERIAL_MANAGE | 停用 |
| **材料規格** | | | |
| GET | `/v1/auth/material/specs` | MATERIAL_VIEW | 規格列表+篩選 |
| GET | `/v1/auth/material/specs/{id}` | MATERIAL_VIEW | 規格詳情 |
| POST | `/v1/auth/material/specs` | MATERIAL_MANAGE | 新增規格 |
| PUT | `/v1/auth/material/specs/{id}` | MATERIAL_MANAGE | 編輯 |
| **廠商** | | | |
| GET | `/v1/auth/material/suppliers` | MATERIAL_VIEW | 廠商列表 |
| POST | `/v1/auth/material/suppliers` | MATERIAL_MANAGE | 新增 |
| PUT | `/v1/auth/material/suppliers/{id}` | MATERIAL_MANAGE | 編輯 |
| **庫存** | | | |
| GET | `/v1/auth/material/inventory` | INVENTORY_VIEW | 庫存總覽+篩選 |
| GET | `/v1/auth/material/inventory/summary` | INVENTORY_VIEW | 庫存統計（按類別） |
| GET | `/v1/auth/material/inventory/alerts` | INVENTORY_VIEW | 低安全庫存預警清單 |
| **合格材料** | | | |
| GET | `/v1/auth/material/approved-materials` | MATERIAL_VIEW | 合格材料列表 |
| GET | `/v1/auth/material/approved-materials/{id}` | MATERIAL_VIEW | 材料詳情 |
| POST | `/v1/auth/material/approved-materials` | MATERIAL_MANAGE | 新增 |
| PUT | `/v1/auth/material/approved-materials/{id}` | MATERIAL_MANAGE | 編輯 |
| POST | `/v1/auth/material/approved-materials/import` | MATERIAL_MANAGE | 批次匯入 |

---

## Step 7：V41 — 採購 + 收料 + 領料 + 出料 + 盤點 + 廢品 + 選單權限

### 7-1 Flyway Migration

**檔案**：`V41__material__create_operation_tables.sql`

**建表順序**（依 FK 依賴）：
1. `purchase_orders`（FK → suppliers, contracts）
2. `purchase_items`（FK → purchase_orders, material_specs）
3. `receiving_records`（FK → purchase_orders, warehouses, material_specs）
4. `issue_requests`（FK → repair_tickets；`replacement_order_id` nullable 無 FK，Phase 4 補 FK）
5. `issue_records`（FK → issue_requests, inventory, material_specs）
6. `inventory_adjustments`（FK → inventory）
7. `disposal_records`（FK → material_specs）
8. Menu：使用 sequence auto-generate（DO $$ ... RETURNING 模式，不硬編 ID）
   - 材料管理 (DIRECTORY)
   - 材料規格 (PAGE)
   - 庫存管理 (PAGE)
   - 採購管理 (PAGE)
   - 合格材料 (PAGE)
9. Permission：MATERIAL_VIEW, MATERIAL_MANAGE, INVENTORY_VIEW, INVENTORY_MANAGE
10. Role binding

### 7-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| **Entities** | | | |
| 1 | Entity | `PurchaseOrder.java` | `TenantAware`, FK → Supplier, Contract |
| 2 | Entity | `PurchaseOrderStatus.java` (enum) | DRAFT, SUBMITTED, APPROVED, RECEIVING, COMPLETED, CANCELLED |
| 3 | Entity | `PurchaseItem.java` | FK → PurchaseOrder, MaterialSpec |
| 4 | Entity | `ReceivingRecord.java` | `TenantAware`, FK → PurchaseOrder, Warehouse, MaterialSpec |
| 5 | Entity | `IssueRequest.java` | `TenantAware`, FK → RepairTicket（nullable）；`replacementOrderId` nullable 無 FK |
| 6 | Entity | `IssueRequestStatus.java` (enum) | PENDING, APPROVED, ISSUED, REJECTED |
| 7 | Entity | `IssueRecord.java` | `TenantAware`, FK → IssueRequest, Inventory, MaterialSpec |
| 8 | Entity | `InventoryAdjustment.java` | `TenantAware`, FK → Inventory |
| 9 | Entity | `AdjustmentType.java` (enum) | COUNT, TRANSFER, CORRECTION, DISPOSAL |
| 10 | Entity | `DisposalRecord.java` | `TenantAware`, FK → MaterialSpec |
| 11 | Entity | `DisposalType.java` (enum) | RETURN_WAREHOUSE, SCRAP |
| **Repositories** | | | |
| 12 | Repository | `PurchaseOrderRepository.java` | `TenantScopedRepository` |
| 13 | Repository | `PurchaseItemRepository.java` | |
| 14 | Repository | `ReceivingRecordRepository.java` | `TenantScopedRepository` |
| 15 | Repository | `IssueRequestRepository.java` | `TenantScopedRepository` |
| 16 | Repository | `IssueRecordRepository.java` | `TenantScopedRepository` |
| 17 | Repository | `InventoryAdjustmentRepository.java` | `TenantScopedRepository` |
| 18 | Repository | `DisposalRecordRepository.java` | `TenantScopedRepository` |
| **DTOs** | | | |
| 19 | DTO | `PurchaseOrderRequest.java` | supplierId, contractId, items[] |
| 20 | DTO | `PurchaseOrderResponse.java` | 含 items, supplierName, totalAmount |
| 21 | DTO | `PurchaseItemRequest.java` / `PurchaseItemResponse.java` | |
| 22 | DTO | `ReceivingRequest.java` / `ReceivingResponse.java` | |
| 23 | DTO | `IssueRequestRequest.java` / `IssueRequestResponse.java` | |
| 24 | DTO | `IssueRecordResponse.java` | |
| 25 | DTO | `InventoryAdjustmentRequest.java` / `InventoryAdjustmentResponse.java` | |
| 26 | DTO | `DisposalRequest.java` / `DisposalResponse.java` | |
| **Services** | | | |
| 27 | Service | `PurchaseOrderService.java` | 見 §7-3 |
| 28 | Service | `ReceivingService.java` | 見 §7-4 |
| 29 | Service | `IssueService.java` | 見 §7-5（手動領料申請，E6 自動觸發在 Phase 4 啟用） |
| 30 | Service | `InventoryAdjustmentService.java` | 見 §7-6 |
| 31 | Service | `DisposalService.java` | |
| **Controllers** | | | |
| 32 | Controller | `PurchaseOrderController.java` | |
| 33 | Controller | `ReceivingController.java` | |
| 34 | Controller | `IssueController.java` | |
| 35 | Controller | `InventoryAdjustmentController.java` | |
| 36 | Controller | `DisposalController.java` | |

### 7-3 採購流程

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepo;
    private final PurchaseItemRepository purchaseItemRepo;
    private final AtomicLong dailySequence = new AtomicLong(1);
    private volatile String lastDate = "";

    @Transactional
    @AuditEvent(AuditEventType.CREATE_PURCHASE_ORDER)
    public PurchaseOrder create(PurchaseOrderRequest request) {
        PurchaseOrder po = PurchaseOrder.builder()
            .poNumber(generatePoNumber())  // PO-20260423-001
            .supplierId(request.getSupplierId())
            .contractId(request.getContractId())
            .orderDate(LocalDate.now())
            .status(PurchaseOrderStatus.DRAFT)
            .createdBy(SecurityContextUtils.getCurrentUserId())
            .build();

        PurchaseOrder saved = poRepo.save(po);

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.getItems()) {
            PurchaseItem item = PurchaseItem.builder()
                .poId(saved.getId())
                .materialSpecId(itemReq.getMaterialSpecId())
                .quantity(itemReq.getQuantity())
                .unitPrice(itemReq.getUnitPrice())
                .build();
            purchaseItemRepo.save(item);
            total = total.add(itemReq.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        saved.setTotalAmount(total);
        return poRepo.save(saved);
    }

    private synchronized String generatePoNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("PO-%s-%03d", today, dailySequence.getAndIncrement());
    }
}
```

### 7-4 收料入庫

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceivingService {

    private final ReceivingRecordRepository receivingRepo;
    private final InventoryRepository inventoryRepo;

    /**
     * 收料：採購到貨入庫
     * 同步更新 inventory.quantity_on_hand
     */
    @Transactional
    @AuditEvent(AuditEventType.RECEIVE_MATERIAL)
    public ReceivingRecord receive(ReceivingRequest request) {
        // 1. 建立收料紀錄
        ReceivingRecord record = ReceivingRecord.builder()
            .poId(request.getPoId())
            .warehouseId(request.getWarehouseId())
            .materialSpecId(request.getMaterialSpecId())
            .quantity(request.getQuantity())
            .receivedDate(LocalDate.now())
            .deliveryNote(request.getDeliveryNote())
            .receivedBy(SecurityContextUtils.getCurrentUsername())
            .build();
        ReceivingRecord saved = receivingRepo.save(record);

        // 2. 更新庫存（UPSERT）
        Inventory inventory = inventoryRepo
            .findByTenantAndWarehouseAndSpec(
                TenantContext.getCurrentTenantId(),
                request.getWarehouseId(),
                request.getMaterialSpecId())
            .orElseGet(() -> Inventory.builder()
                .warehouseId(request.getWarehouseId())
                .materialSpecId(request.getMaterialSpecId())
                .quantityOnHand(0)
                .safetyStock(0)
                .build());

        inventory.setQuantityOnHand(
            inventory.getQuantityOnHand() + request.getQuantity());
        inventoryRepo.save(inventory);

        // 3. 更新採購單狀態
        updatePurchaseOrderStatus(request.getPoId());

        return saved;
    }
}
```

### 7-5 領料出庫

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRequestRepository issueRequestRepo;
    private final IssueRecordRepository issueRecordRepo;
    private final InventoryRepository inventoryRepo;

    /**
     * 手動建立領料申請（Phase 3 僅支援手動）
     * Phase 4 完成後可由 E6 事件自動觸發
     */
    @Transactional
    public IssueRequest createManual(IssueRequestRequest request) {
        return issueRequestRepo.save(IssueRequest.builder()
            .requestNumber(generateRequestNumber())
            .repairTicketId(request.getRepairTicketId())      // nullable
            .replacementOrderId(request.getReplacementOrderId()) // nullable，Phase 4 才有值
            .requestedBy(SecurityContextUtils.getCurrentUserId())
            .status(IssueRequestStatus.PENDING)
            .build());
    }

    /**
     * 審核通過 → 實際出庫（E7）
     * 扣減 inventory.quantity_on_hand
     */
    @Transactional
    @AuditEvent(AuditEventType.ISSUE_MATERIAL)
    public void issue(Long requestId, List<IssueRecordRequest> items) {
        IssueRequest request = findRequestOrThrow(requestId);
        validateStatus(request, IssueRequestStatus.APPROVED);

        for (var item : items) {
            Inventory inventory = inventoryRepo.findById(item.getInventoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

            if (inventory.getQuantityOnHand() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
            }

            // 扣庫
            inventory.setQuantityOnHand(
                inventory.getQuantityOnHand() - item.getQuantity());
            inventoryRepo.save(inventory);

            // 記錄出料
            IssueRecord record = IssueRecord.builder()
                .requestId(requestId)
                .inventoryId(item.getInventoryId())
                .materialSpecId(item.getMaterialSpecId())
                .quantity(item.getQuantity())
                .issuedBy(SecurityContextUtils.getCurrentUsername())
                .build();
            issueRecordRepo.save(record);
        }

        request.setStatus(IssueRequestStatus.ISSUED);
        issueRequestRepo.save(request);
    }

    /**
     * 結案扣庫確認（Phase 4 E10 呼叫）
     * 驗證領料數量與實際使用是否一致
     */
    public void confirmDeduction(Long replacementOrderId) {
        // Phase 4 啟用：比對 replacement_items 實際使用量
        // 差異量做 inventory_adjustment (CORRECTION)
    }
}
```

### 7-6 庫存盤點/調整

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAdjustmentService {

    private final InventoryAdjustmentRepository adjustmentRepo;
    private final InventoryRepository inventoryRepo;

    /**
     * 盤點：輸入實際數量，自動計算差異
     */
    @Transactional
    @AuditEvent(AuditEventType.ADJUST_INVENTORY)
    public InventoryAdjustment count(Long inventoryId, int actualQuantity, String reason) {
        Inventory inventory = inventoryRepo.findById(inventoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

        int diff = actualQuantity - inventory.getQuantityOnHand();

        InventoryAdjustment adj = InventoryAdjustment.builder()
            .inventoryId(inventoryId)
            .adjustmentType(AdjustmentType.COUNT)
            .quantityChange(diff)
            .reason(reason)
            .adjustedBy(SecurityContextUtils.getCurrentUsername())
            .build();
        InventoryAdjustment saved = adjustmentRepo.save(adj);

        inventory.setQuantityOnHand(actualQuantity);
        inventoryRepo.save(inventory);

        return saved;
    }

    /**
     * 轉庫：從 A 庫轉到 B 庫
     */
    @Transactional
    public void transfer(Long fromInventoryId, Long toWarehouseId, int quantity, String reason) {
        Inventory from = inventoryRepo.findById(fromInventoryId).orElseThrow();
        if (from.getQuantityOnHand() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
        }

        // 出庫
        from.setQuantityOnHand(from.getQuantityOnHand() - quantity);
        inventoryRepo.save(from);
        adjustmentRepo.save(InventoryAdjustment.builder()
            .inventoryId(fromInventoryId)
            .adjustmentType(AdjustmentType.TRANSFER)
            .quantityChange(-quantity)
            .reason(reason)
            .adjustedBy(SecurityContextUtils.getCurrentUsername())
            .build());

        // 入庫（UPSERT）
        Inventory to = inventoryRepo.findByTenantAndWarehouseAndSpec(
                TenantContext.getCurrentTenantId(), toWarehouseId, from.getMaterialSpecId())
            .orElseGet(() -> Inventory.builder()
                .warehouseId(toWarehouseId)
                .materialSpecId(from.getMaterialSpecId())
                .quantityOnHand(0)
                .safetyStock(0)
                .build());
        to.setQuantityOnHand(to.getQuantityOnHand() + quantity);
        inventoryRepo.save(to);
        adjustmentRepo.save(InventoryAdjustment.builder()
            .inventoryId(to.getId())
            .adjustmentType(AdjustmentType.TRANSFER)
            .quantityChange(quantity)
            .reason(reason)
            .adjustedBy(SecurityContextUtils.getCurrentUsername())
            .build());
    }
}
```

### 7-7 API 端點（操作表）

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| **採購** | | | |
| GET | `/v1/auth/material/purchase-orders` | MATERIAL_VIEW | 採購單列表 |
| GET | `/v1/auth/material/purchase-orders/{id}` | MATERIAL_VIEW | 採購單詳情 |
| POST | `/v1/auth/material/purchase-orders` | MATERIAL_MANAGE | 新增採購單 |
| PUT | `/v1/auth/material/purchase-orders/{id}` | MATERIAL_MANAGE | 編輯 |
| POST | `/v1/auth/material/purchase-orders/{id}/submit` | MATERIAL_MANAGE | 送審 |
| **收料** | | | |
| GET | `/v1/auth/material/receiving` | MATERIAL_VIEW | 收料紀錄列表 |
| POST | `/v1/auth/material/receiving` | MATERIAL_MANAGE | 新增收料 |
| **領料** | | | |
| GET | `/v1/auth/material/issue-requests` | MATERIAL_VIEW | 領料申請列表 |
| POST | `/v1/auth/material/issue-requests` | MATERIAL_MANAGE | 新增領料申請 |
| POST | `/v1/auth/material/issue-requests/{id}/approve` | MATERIAL_MANAGE | 核准 |
| POST | `/v1/auth/material/issue-requests/{id}/issue` | MATERIAL_MANAGE | 出料 |
| POST | `/v1/auth/material/issue-requests/{id}/reject` | MATERIAL_MANAGE | 駁回 |
| **盤點/調整** | | | |
| GET | `/v1/auth/material/adjustments` | INVENTORY_VIEW | 調整紀錄列表 |
| POST | `/v1/auth/material/adjustments/count` | INVENTORY_MANAGE | 盤點 |
| POST | `/v1/auth/material/adjustments/transfer` | INVENTORY_MANAGE | 轉庫 |
| POST | `/v1/auth/material/adjustments/correction` | INVENTORY_MANAGE | 修正 |
| **廢品** | | | |
| GET | `/v1/auth/material/disposals` | MATERIAL_VIEW | 廢品紀錄列表 |
| POST | `/v1/auth/material/disposals` | MATERIAL_MANAGE | 新增廢品處理 |

### 7-8 審計事件

`AuditCategory` 新增 `MATERIAL`，`AuditEventType` 新增：
- `CREATE_PURCHASE_ORDER`, `RECEIVE_MATERIAL`, `ISSUE_MATERIAL`
- `ADJUST_INVENTORY`, `DISPOSE_MATERIAL`, `IMPORT_APPROVED_MATERIAL`

### 7-9 ErrorCode 新增

```java
MATERIAL_SPEC_NOT_FOUND(85001),
INSUFFICIENT_INVENTORY(85002),
INVENTORY_NOT_FOUND(85003),
WAREHOUSE_NOT_FOUND(85004),
SUPPLIER_NOT_FOUND(85005),
MATERIAL_NOT_APPROVED(85010),
MATERIAL_NOT_AVAILABLE(85011),
```

### 7-10 前端

| # | 檔案 | 說明 |
|---|------|------|
| 1 | `src/types/material.ts` | Warehouse, MaterialSpec, Supplier, Inventory, PurchaseOrder, IssueRequest, ApprovedMaterial 等型別 |
| 2 | `src/api/material/index.ts` | 材料管理全套 API |
| 3 | `src/stores/materialStore.ts` | 庫存統計 + 安全庫存預警快取 |
| **材料規格** | | |
| 4 | `src/views/admin/material/MaterialSpecView.vue` | 材料規格列表+篩選+分頁 |
| 5 | `src/views/admin/material/MaterialSpecDialog.vue` | 新增/編輯材料規格（含 JSONB attributes） |
| **庫存** | | |
| 6 | `src/views/admin/material/InventoryView.vue` | 庫存總覽+篩選+安全庫存預警標記 |
| 7 | `src/views/admin/material/InventorySummaryView.vue` | 庫存統計圖表（按類別、庫別、趨勢） |
| 8 | `src/views/admin/material/InventoryAlertView.vue` | 低庫存預警清單 |
| **採購** | | |
| 9 | `src/views/admin/material/PurchaseOrderView.vue` | 採購單列表 |
| 10 | `src/views/admin/material/PurchaseOrderDetailView.vue` | 採購單詳情+明細 |
| 11 | `src/views/admin/material/ReceivingView.vue` | 收料紀錄 |
| **領料/出料** | | |
| 12 | `src/views/admin/material/IssueRequestView.vue` | 領料申請列表 |
| 13 | `src/views/admin/material/IssueDialog.vue` | 出料操作（選擇庫別+數量） |
| **盤點/調整** | | |
| 14 | `src/views/admin/material/InventoryAdjustmentView.vue` | 盤點/轉庫/修正紀錄 |
| 15 | `src/views/admin/material/StockCountDialog.vue` | 盤點 Dialog |
| 16 | `src/views/admin/material/TransferDialog.vue` | 轉庫 Dialog |
| **廢品** | | |
| 17 | `src/views/admin/material/DisposalView.vue` | 廢品/繳庫紀錄 |
| **合格材料** | | |
| 18 | `src/views/admin/material/ApprovedMaterialView.vue` | 合格材料列表 + 匯入 |
| **基礎設定** | | |
| 19 | `src/views/admin/material/WarehouseView.vue` | 庫別管理 |
| 20 | `src/views/admin/material/SupplierView.vue` | 廠商管理 |
| **共用** | | |
| 21 | `src/components/MaterialSelector.vue` | **共用元件**：合格材料下拉選擇（含規格搜尋），Phase 4 換裝也會用到 |
| 22 | i18n | zh-TW / en / zh-CN 新增 material 相關 key |
| 23 | router | 新增 /admin/material/* 路由 |

#### 庫存總覽頁面佈局

```
┌──────────────────────────────────────────────────────────────┐
│  庫存管理                                                     │
├──────────────────────────────────────────────────────────────┤
│  📊 統計卡片                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 總品項    │ │ 總數量   │ │ 低庫存⚠  │ │ 今日異動  │       │
│  │   45      │ │  12,340  │ │    3     │ │   +15,-8 │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
├──────────────────────────────────────────────────────────────┤
│  篩選：[庫別▾] [材料類別▾] [關鍵字 🔍] [☐ 僅顯示低庫存]      │
├──────────────────────────────────────────────────────────────┤
│  Table:                                                       │
│  │ 庫別   │ 規格代碼 │ 材料名稱   │ 類別  │ 庫存 │ 安全量│ 狀態│
│  │ 主庫   │ M-001   │ LED燈具150W │ 燈具  │  25  │  10  │ ✅ │
│  │ 主庫   │ M-002   │ LED燈具200W │ 燈具  │   3  │  10  │ ⚠️ │
│  │ 南區庫 │ M-003   │ 燈桿 6M    │ 燈桿  │  12  │   5  │ ✅ │
│  ...                                                          │
└──────────────────────────────────────────────────────────────┘
```

### 7-11 單元測試

| # | 測試類 | 涵蓋範圍 |
|---|--------|----------|
| 1 | `WarehouseServiceTest` | CRUD |
| 2 | `MaterialSpecServiceTest` | CRUD + 分類篩選 |
| 3 | `SupplierServiceTest` | CRUD |
| 4 | `InventoryServiceTest` | 查詢 + 安全庫存 + 統計 |
| 5 | `ApprovedMaterialServiceTest` | CRUD + 批次匯入 + 狀態管理 |
| 6 | `PurchaseOrderServiceTest` | 建立+明細+狀態流轉 |
| 7 | `ReceivingServiceTest` | 收料+庫存更新（UPSERT）+ 採購單狀態更新 |
| 8 | `IssueServiceTest` | 手動建立申請 + 審核 + 出庫+庫存扣減 + 庫存不足例外 |
| 9 | `InventoryAdjustmentServiceTest` | 盤點+轉庫+修正 |
| 10 | `DisposalServiceTest` | 廢品處理 |
| 11 | `PurchaseOrderControllerTest` | 端點+權限 |
| 12 | `IssueControllerTest` | 端點+權限 |
| 13 | `InventoryControllerTest` | 端點+權限 |
| 14 | `ApprovedMaterialControllerTest` | 端點+權限+匯入 |

---

## Phase 3 完成標準

- [ ] V40 migration 執行成功，warehouses / material_specs / suppliers / inventory / approved_materials 表建立
- [ ] V41 migration 執行成功，purchase + receiving + issue + adjustment + disposal 表 + 選單權限
- [ ] 庫別/材料規格/廠商 CRUD 可用
- [ ] 庫存查詢+統計+安全庫存預警可用
- [ ] 合格材料 CRUD + 批次匯入可用
- [ ] 採購流程可用（建立→明細→收料→庫存增加）
- [ ] 領料流程可用（手動申請→審核→出庫→庫存扣減）
- [ ] 庫存盤點/轉庫/修正可用
- [ ] 廢品處理可用
- [ ] E12 事件：每日安全庫存排程→低庫存預警（驗證）
- [ ] 前端材料管理全套頁面可用
- [ ] 共用元件 MaterialSelector 可用
- [ ] 所有單元測試通過
- [ ] `mvn test` 全過
