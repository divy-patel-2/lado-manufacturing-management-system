
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
