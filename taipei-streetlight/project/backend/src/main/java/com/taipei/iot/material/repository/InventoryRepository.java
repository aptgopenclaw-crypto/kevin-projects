package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, TenantScopedRepository {

    @Query("""
        SELECT i FROM Inventory i
        JOIN FETCH i.warehouse w
        JOIN FETCH i.materialSpec ms
        WHERE (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
          AND (:category IS NULL OR ms.category = :category)
          AND (:belowSafetyStock = false OR i.quantityOnHand < i.safetyStock)
          AND (CAST(:keyword AS string) IS NULL
               OR LOWER(ms.specCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(ms.specName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY ms.specCode
        """)
    Page<Inventory> findByFilters(@Param("warehouseId") Long warehouseId,
                                   @Param("category") MaterialCategory category,
                                   @Param("keyword") String keyword,
                                   @Param("belowSafetyStock") boolean belowSafetyStock,
                                   Pageable pageable);

    @Query("""
        SELECT i FROM Inventory i
        JOIN FETCH i.warehouse w
        JOIN FETCH i.materialSpec ms
        WHERE i.quantityOnHand < i.safetyStock AND i.safetyStock > 0
        """)
    List<Inventory> findBelowSafetyStock();

    @Query(value = """
        SELECT i.* FROM inventory i
        WHERE i.quantity_on_hand < i.safety_stock AND i.safety_stock > 0
        """, nativeQuery = true)
    List<Inventory> findBelowSafetyStockAllTenants();

    @Query("""
        SELECT i FROM Inventory i
        WHERE i.tenantId = :tenantId
          AND i.warehouseId = :warehouseId
          AND i.materialSpecId = :materialSpecId
        """)
    Optional<Inventory> findByTenantAndWarehouseAndSpec(
            @Param("tenantId") String tenantId,
            @Param("warehouseId") Long warehouseId,
            @Param("materialSpecId") Long materialSpecId);

    @Query("""
        SELECT ms.category, COUNT(i), SUM(i.quantityOnHand)
        FROM Inventory i JOIN i.materialSpec ms
        GROUP BY ms.category
        """)
    List<Object[]> summarizeByCategory();
}
