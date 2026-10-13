
package lado.com.lado_manufacturing;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createAccount(
            Long companyId,
            String accountName,
            String accountType,
            BigDecimal openingBalance) {

        String duplicateSql = """
            SELECT COUNT(*)
            FROM ACCOUNTS
            WHERE COMPANY_ID = ?
              AND UPPER(ACCOUNT_NAME) = UPPER(?)
            """;
        Integer duplicates = jdbcTemplate.queryForObject(
            duplicateSql, Integer.class, companyId, accountName
        );
        if (duplicates != null && duplicates > 0) {
            throw new IllegalArgumentException(
                "An account with this name already exists."
            );
        }

        String sql = """
            INSERT INTO ACCOUNTS
                (COMPANY_ID, ACCOUNT_NAME, ACCOUNT_TYPE,
                 OPENING_BALANCE, CURRENT_BALANCE, IS_ACTIVE)
            VALUES (?, ?, ?, ?, ?, 'Y')
            """;
        jdbcTemplate.update(
            sql, companyId, accountName, accountType,
            openingBalance, openingBalance
        );
    }

    public List<Account> findAllAccounts(Long companyId) {
        String sql = """
            SELECT ACCOUNT_ID, COMPANY_ID, ACCOUNT_NAME, ACCOUNT_TYPE,
                   OPENING_BALANCE, CURRENT_BALANCE, IS_ACTIVE
            FROM ACCOUNTS
            WHERE COMPANY_ID = ?
            ORDER BY ACCOUNT_NAME
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new Account(
            rs.getLong("ACCOUNT_ID"),
            rs.getLong("COMPANY_ID"),
            rs.getString("ACCOUNT_NAME"),
            rs.getString("ACCOUNT_TYPE"),
            rs.getBigDecimal("OPENING_BALANCE"),
            rs.getBigDecimal("CURRENT_BALANCE"),
            rs.getString("IS_ACTIVE")
        ), companyId);
    }

    public List<Map<String, Object>> findAllTransactions(Long companyId) {
        String sql = """
            SELECT
                ft.TRANSACTION_ID,
                ft.COMPANY_ID,
                ft.ACCOUNT_ID,
                a.ACCOUNT_NAME,
                a.ACCOUNT_TYPE,
                ft.TRANSACTION_TYPE,
                ft.AMOUNT,
                ft.TRANSACTION_DATE,
                ft.REFERENCE_TYPE,
                ft.REFERENCE_ID,
                ft.DESCRIPTION
            FROM FINANCIAL_TRANSACTIONS ft
            JOIN ACCOUNTS a
              ON a.ACCOUNT_ID = ft.ACCOUNT_ID
             AND a.COMPANY_ID = ft.COMPANY_ID
            WHERE ft.COMPANY_ID = ?
            ORDER BY ft.TRANSACTION_DATE DESC, ft.TRANSACTION_ID DESC
            """;

        return jdbcTemplate.queryForList(sql, companyId);
    }

    public List<Account> findPaymentAccounts(Long companyId) {
        String sql = """
            SELECT ACCOUNT_ID, COMPANY_ID, ACCOUNT_NAME, ACCOUNT_TYPE,
                   OPENING_BALANCE, CURRENT_BALANCE, IS_ACTIVE
            FROM ACCOUNTS
            WHERE COMPANY_ID = ?
              AND IS_ACTIVE = 'Y'
              AND ACCOUNT_TYPE IN ('CASH', 'BANK')
            ORDER BY ACCOUNT_NAME
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new Account(
            rs.getLong("ACCOUNT_ID"),
            rs.getLong("COMPANY_ID"),
            rs.getString("ACCOUNT_NAME"),
            rs.getString("ACCOUNT_TYPE"),
            rs.getBigDecimal("OPENING_BALANCE"),
            rs.getBigDecimal("CURRENT_BALANCE"),
            rs.getString("IS_ACTIVE")
        ), companyId);
    }

    public boolean isValidPaymentAccount(Long accountId, Long companyId) {
        String sql = """
            SELECT COUNT(*)
            FROM ACCOUNTS
            WHERE ACCOUNT_ID = ?
              AND COMPANY_ID = ?
              AND IS_ACTIVE = 'Y'
              AND ACCOUNT_TYPE IN ('CASH', 'BANK')
            """;

        Integer count = jdbcTemplate.queryForObject(
            sql, Integer.class, accountId, companyId
        );

        return count != null && count > 0;
    }

    @Transactional
    public void addTransaction(
            Long companyId,
            Long accountId,
            String transactionType,
            BigDecimal amount,
            String description,
            LocalDate transactionDate) {

        if (!"INCOME".equals(transactionType)
                && !"EXPENSE".equals(transactionType)) {
            throw new IllegalArgumentException(
                "Transaction type must be INCOME or EXPENSE."
            );
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                "Amount must be greater than zero."
            );
        }

        if (!isValidPaymentAccount(accountId, companyId)) {
            throw new IllegalArgumentException(
                "The selected account is invalid or inactive."
            );
        }

        String sql = """
            INSERT INTO FINANCIAL_TRANSACTIONS
                (COMPANY_ID, ACCOUNT_ID, TRANSACTION_TYPE,
                 AMOUNT, TRANSACTION_DATE, REFERENCE_TYPE, DESCRIPTION)
            VALUES (?, ?, ?, ?, ?, 'MANUAL', ?)
            """;

        jdbcTemplate.update(
            sql,
            companyId,
            accountId,
            transactionType,
            amount,
            Date.valueOf(
                transactionDate != null ? transactionDate : LocalDate.now()
            ),
            description
        );

        // TRG_FT_BALANCE updates CURRENT_BALANCE automatically.
    }

    @Transactional
    public void transferFunds(
            Long companyId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            String description,
            LocalDate transactionDate) {

        if (fromAccountId == null || toAccountId == null
                || fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                "Select two different accounts."
            );
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                "Transfer amount must be greater than zero."
            );
        }

        if (!isValidPaymentAccount(fromAccountId, companyId)
                || !isValidPaymentAccount(toAccountId, companyId)) {
            throw new IllegalArgumentException(
                "Both accounts must be active Cash or Bank accounts "
                + "belonging to your company."
            );
        }

        LocalDate date = transactionDate != null
            ? transactionDate : LocalDate.now();

        String transferDescription =
            description == null || description.isBlank()
                ? "Account transfer"
                : description.trim();

        insertTransferRow(
            companyId, fromAccountId, "EXPENSE", amount,
            Date.valueOf(date), transferDescription
        );

        insertTransferRow(
            companyId, toAccountId, "INCOME", amount,
            Date.valueOf(date), transferDescription
        );

        // The transaction is atomic: both rows commit, or neither does.
        // TRG_FT_BALANCE updates both account balances.
    }

    private void insertTransferRow(
            Long companyId,
            Long accountId,
            String type,
            BigDecimal amount,
            Date date,
            String description) {

        String sql = """
            INSERT INTO FINANCIAL_TRANSACTIONS
                (COMPANY_ID, ACCOUNT_ID, TRANSACTION_TYPE,
                 AMOUNT, TRANSACTION_DATE, REFERENCE_TYPE, DESCRIPTION)
            VALUES (?, ?, ?, ?, ?, 'TRANSFER', ?)
            """;

        jdbcTemplate.update(
            sql, companyId, accountId, type, amount, date, description
        );
    }
}
