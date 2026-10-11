
package lado.com.lado_manufacturing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // INVENTORY REPORT
    public List<Map<String, Object>> getInventoryReport(
            Long companyId) {

        String sql = """
            SELECT
                p.PRODUCT_ID,
                p.PRODUCT_CODE,
                p.PRODUCT_NAME,
                NVL(i.CURRENT_STOCK, 0) AS CURRENT_STOCK,
                NVL(i.MIN_STOCK, 0) AS MIN_STOCK,
                p.COST_PRICE,
                p.SELLING_PRICE,
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

    // PURCHASE REPORT
    public List<Map<String, Object>> getPurchaseReport(
            Long companyId,
            LocalDate fromDate,
            LocalDate toDate) {

        String sql = """
            SELECT
                p.PURCHASE_ID,
                p.PURCHASE_NO,
                p.SUPPLIER_ID,
                s.SUPPLIER_NAME,
                p.PURCHASE_DATE,
                p.TOTAL_AMOUNT,
                p.PAYMENT_STATUS
            FROM PURCHASES p
            LEFT JOIN SUPPLIERS s
                ON s.SUPPLIER_ID = p.SUPPLIER_ID
                AND s.COMPANY_ID = p.COMPANY_ID
            WHERE p.COMPANY_ID = ?
              AND (? IS NULL OR p.PURCHASE_DATE >= ?)
              AND (? IS NULL OR p.PURCHASE_DATE < ?)
            ORDER BY
                p.PURCHASE_DATE DESC,
                p.PURCHASE_ID DESC
            """;

        Date from = fromDate == null
                ? null : Date.valueOf(fromDate);

        Date toExclusive = toDate == null
                ? null : Date.valueOf(toDate.plusDays(1));

        return jdbcTemplate.queryForList(
                sql,
                companyId,
                from,
                from,
                toExclusive,
                toExclusive
        );
    }

    // PAYMENT REPORT
    public List<Map<String, Object>> getPaymentReport(
            Long companyId,
            LocalDate fromDate,
            LocalDate toDate) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                p.PAYMENT_ID,
                p.PAYMENT_TYPE,
                p.PARTY_TYPE,
                p.PARTY_ID,
                CASE
                    WHEN UPPER(p.PARTY_TYPE) = 'CUSTOMER'
                        THEN c.CUSTOMER_NAME
                    WHEN UPPER(p.PARTY_TYPE) = 'SUPPLIER'
                        THEN s.SUPPLIER_NAME
                    ELSE NULL
                END AS PARTY_NAME,
                p.ACCOUNT_ID,
                a.ACCOUNT_NAME,
                p.AMOUNT,
                p.PAYMENT_DATE,
                p.PAYMENT_MODE,
                p.REFERENCE_NO,
                p.NOTES
            FROM PAYMENTS p
            LEFT JOIN CUSTOMERS c
                ON UPPER(p.PARTY_TYPE) = 'CUSTOMER'
                AND c.CUSTOMER_ID = p.PARTY_ID
                AND c.COMPANY_ID = p.COMPANY_ID
            LEFT JOIN SUPPLIERS s
                ON UPPER(p.PARTY_TYPE) = 'SUPPLIER'
                AND s.SUPPLIER_ID = p.PARTY_ID
                AND s.COMPANY_ID = p.COMPANY_ID
            LEFT JOIN ACCOUNTS a
                ON a.ACCOUNT_ID = p.ACCOUNT_ID
                AND a.COMPANY_ID = p.COMPANY_ID
            WHERE p.COMPANY_ID = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(companyId);

        if (fromDate != null) {
            sql.append(" AND p.PAYMENT_DATE >= ?");
            params.add(Date.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND p.PAYMENT_DATE < ?");
            params.add(Date.valueOf(toDate.plusDays(1)));
        }

        sql.append(
                " ORDER BY p.PAYMENT_DATE DESC, p.PAYMENT_ID DESC"
        );

        return jdbcTemplate.queryForList(
                sql.toString(),
                params.toArray()
        );
    }
}
