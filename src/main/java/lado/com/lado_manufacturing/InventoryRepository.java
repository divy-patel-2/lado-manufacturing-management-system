package lado.com.lado_manufacturing;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Fetch active products and inventory for a specific company.
    public List<Map<String, Object>> findAllInventory(Long companyId) {

        String sql = """
            SELECT
                p.PRODUCT_ID,
                p.COMPANY_ID,
                p.PRODUCT_CODE,
                p.PRODUCT_NAME,
                p.CATEGORY_ID,
                p.UNIT_ID,
                p.SELLING_PRICE,
                p.COST_PRICE,
                p.REORDER_LEVEL,
                NVL(i.INVENTORY_ID, 0) AS INVENTORY_ID,
                NVL(i.CURRENT_STOCK, 0) AS CURRENT_STOCK,
                NVL(i.MIN_STOCK, 0) AS MIN_STOCK,
                i.LAST_UPDATED,
                CASE
                    WHEN NVL(i.CURRENT_STOCK, 0) = 0
                        THEN 'OUT_OF_STOCK'
                    WHEN NVL(i.CURRENT_STOCK, 0)
                         <= GREATEST(
                             NVL(i.MIN_STOCK, 0),
                             NVL(p.REORDER_LEVEL, 0)
                         )
                        THEN 'LOW_STOCK'
                    ELSE 'IN_STOCK'
                END AS STOCK_STATUS
            FROM PRODUCTS p
            LEFT JOIN INVENTORY i
                ON i.PRODUCT_ID = p.PRODUCT_ID
                AND i.COMPANY_ID = p.COMPANY_ID
            WHERE p.COMPANY_ID = ?
                AND p.IS_ACTIVE = 'Y'
            ORDER BY p.PRODUCT_NAME
            """;

        return jdbcTemplate.queryForList(sql, companyId);
    }

    // Verify that the product belongs to the specified company.
    public boolean productExistsForCompany(
            Long companyId,
            Long productId) {

        String sql = """
            SELECT COUNT(*)
            FROM PRODUCTS
            WHERE COMPANY_ID = ?
              AND PRODUCT_ID = ?
              AND IS_ACTIVE = 'Y'
            """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                companyId,
                productId
        );

        return count != null && count > 0;
    }

    // Add or remove stock using the existing Oracle procedure.
    public void applyStockAdjustment(
            Long companyId,
            Long productId,
            BigDecimal delta,
            String source,
            String notes) {

        jdbcTemplate.execute(
            (ConnectionCallback<Void>) connection -> {

                try (java.sql.CallableStatement statement =
                        connection.prepareCall(
                                "{ call APPLY_STOCK(?, ?, ?, ?, ?, ?) }")) {

                    statement.setLong(1, companyId);
                    statement.setLong(2, productId);
                    statement.setBigDecimal(3, delta);
                    statement.setString(4, source);
                    statement.setNull(5, Types.NUMERIC);
                    statement.setString(6, notes);

                    statement.execute();

                    return null;
                }
            }
        );
    }
}
