
package lado.com.lado_manufacturing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAccounts(
            @AuthenticationPrincipal CompanyUserDetails user) {

        if (!isValidUser(user)) {
            return unauthorized();
        }

        return ResponseEntity.ok(
            accountRepository.findAllAccounts(user.getCompanyId())
        );
    }

    @PostMapping
    public ResponseEntity<?> createAccount(
            @AuthenticationPrincipal CompanyUserDetails user,
            @RequestBody Map<String, Object> request) {

        if (!isValidUser(user)) {
            return unauthorized();
        }

        try {
            String accountName = optionalText(request, "accountName");
            String accountType = optionalText(request, "accountType");
            Object rawOpeningBalance = request.get("openingBalance");

            if (accountName == null || accountName.isBlank()) {
                throw new IllegalArgumentException("Account name is required.");
            }
            if (accountName.length() > 100) {
                throw new IllegalArgumentException("Account name cannot exceed 100 characters.");
            }
            if (accountType == null) {
                throw new IllegalArgumentException("Account type is required.");
            }
            accountType = accountType.toUpperCase();
            if (!"CASH".equals(accountType) && !"BANK".equals(accountType)) {
                throw new IllegalArgumentException("Account type must be Cash or Bank.");
            }

            BigDecimal openingBalance;
            try {
                openingBalance = new BigDecimal(
                    rawOpeningBalance == null || rawOpeningBalance.toString().isBlank()
                        ? "0" : rawOpeningBalance.toString()
                );
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Opening balance must be a valid number.");
            }
            if (openingBalance.signum() < 0) {
                throw new IllegalArgumentException("Opening balance cannot be negative.");
            }

            accountRepository.createAccount(
                user.getCompanyId(), accountName.trim(), accountType, openingBalance
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(success("Account created successfully."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Unable to create account. Check whether an account with this name already exists."));
        }
    }

    @GetMapping("/payment-accounts")
    public ResponseEntity<?> getPaymentAccounts(
            @AuthenticationPrincipal CompanyUserDetails user) {

        if (!isValidUser(user)) {
            return unauthorized();
        }

        return ResponseEntity.ok(
            accountRepository.findPaymentAccounts(user.getCompanyId())
        );
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @AuthenticationPrincipal CompanyUserDetails user) {

        if (!isValidUser(user)) {
            return unauthorized();
        }

        return ResponseEntity.ok(
            accountRepository.findAllTransactions(user.getCompanyId())
        );
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> addTransaction(
            @AuthenticationPrincipal CompanyUserDetails user,
            @RequestBody Map<String, Object> request) {

        if (!isValidUser(user)) {
            return unauthorized();
        }

        try {
            Long accountId = requiredLong(request, "accountId");

            // Accept both current UI naming and backend naming.
            String type = optionalText(request, "transactionType");
            if (type == null) {
                type = optionalText(request, "category");
            }
            if (type != null) {
                type = type.toUpperCase();
            }

            BigDecimal amount = requiredAmount(request, "amount");
            String description = optionalText(request, "description");

            if (description != null && description.length() > 500) {
                throw new IllegalArgumentException(
                    "Description cannot exceed 500 characters."
                );
            }

            LocalDate date = parseDate(request.get("transactionDate"));

            accountRepository.addTransaction(
                user.getCompanyId(),
                accountId,
                type,
                amount,
                description,
                date
            );

            return ResponseEntity.ok(
                success("Transaction saved successfully.")
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                error(ex.getMessage())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Unable to save the transaction."));
        }
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> transferFunds(
            @AuthenticationPrincipal CompanyUserDetails user,
            @RequestBody Map<String, Object> request) {

        if (!isValidUser(user)) {
            return unauthorized();
        }

        try {
            Long fromAccountId = requiredLong(request, "fromAccountId");
            Long toAccountId = requiredLong(request, "toAccountId");
            BigDecimal amount = requiredAmount(request, "amount");
            String description = optionalText(request, "description");

            if (description != null && description.length() > 500) {
                throw new IllegalArgumentException(
                    "Description cannot exceed 500 characters."
                );
            }

            LocalDate date = parseDate(request.get("transactionDate"));

            accountRepository.transferFunds(
                user.getCompanyId(),
                fromAccountId,
                toAccountId,
                amount,
                description,
                date
            );

            return ResponseEntity.ok(
                success("Transfer completed successfully.")
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                error(ex.getMessage())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("Unable to complete the transfer."));
        }
    }

    private boolean isValidUser(CompanyUserDetails user) {
        return user != null && user.getCompanyId() != null;
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(error("Please log in to access accounts."));
    }

    private Long requiredLong(
            Map<String, Object> request, String key) {

        Object value = request.get(key);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                key + " is required."
            );
        }

        try {
            long parsed = Long.parseLong(value.toString());
            if (parsed <= 0) {
                throw new IllegalArgumentException(
                    key + " must be a positive ID."
                );
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                key + " must be a valid number."
            );
        }
    }

    private BigDecimal requiredAmount(
            Map<String, Object> request, String key) {

        Object value = request.get(key);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                "Amount is required."
            );
        }

        try {
            BigDecimal amount = new BigDecimal(value.toString());

            if (amount.signum() <= 0) {
                throw new IllegalArgumentException(
                    "Amount must be greater than zero."
                );
            }

            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Amount must be a valid number."
            );
        }
    }

    private String optionalText(
            Map<String, Object> request, String key) {

        Object value = request.get(key);

        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDate parseDate(Object value) {
        if (value == null || value.toString().isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(value.toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                "Transaction date must use YYYY-MM-DD format."
            );
        }
    }

    private Map<String, Object> success(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
