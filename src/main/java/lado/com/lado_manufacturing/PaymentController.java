package lado.com.lado_manufacturing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    public PaymentController(
            PaymentRepository paymentRepository,
            AccountRepository accountRepository) {

        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
    }

    // ==========================================
    // GET ALL PAYMENTS FOR LOGGED-IN COMPANY
    // ==========================================
    @GetMapping
    public ResponseEntity<List<Payment>> getPayments(
            @AuthenticationPrincipal CompanyUserDetails user) {

        requireLogin(user);

        List<Payment> payments =
                paymentRepository
                        .findByCompanyIdOrderByPaymentDateDescPaymentIdDesc(
                                user.getCompanyId()
                        );

        return ResponseEntity.ok(payments);
    }

    // ==========================================
    // CREATE PAYMENT
    // ==========================================
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @AuthenticationPrincipal CompanyUserDetails user,
            @Valid @RequestBody PaymentRequest request) {

        requireLogin(user);
        validateRequest(request);
        validateAccount(request.accountId(), user.getCompanyId());

        Payment payment = new Payment();

        payment.setCompanyId(user.getCompanyId());
        applyRequest(payment, request, false);

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        // Oracle trigger TRG_PAYMENTS_FT creates the linked
        // FINANCIAL_TRANSACTIONS record.
        // TRG_FT_BALANCE updates the account balance.
        // Do not create another financial transaction here.

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedPayment);
    }

    // ==========================================
    // UPDATE EXISTING PAYMENT
    // ==========================================
    @PutMapping("/{paymentId}")
    public ResponseEntity<Payment> updatePayment(
            @AuthenticationPrincipal CompanyUserDetails user,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentRequest request) {

        requireLogin(user);
        validateRequest(request);
        validateAccount(request.accountId(), user.getCompanyId());

        Payment payment = paymentRepository
                .findByPaymentIdAndCompanyId(
                        paymentId,
                        user.getCompanyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found in your company."
                ));

        applyRequest(payment, request, true);

        Payment updatedPayment = paymentRepository.saveAndFlush(payment);

        // Oracle trigger TRG_PAYMENTS_FT updates the linked
        // financial transaction.
        // TRG_FT_BALANCE adjusts the account balance.

        return ResponseEntity.ok(updatedPayment);
    }

    // ==========================================
    // DELETE EXISTING PAYMENT
    // ==========================================
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(
            @AuthenticationPrincipal CompanyUserDetails user,
            @PathVariable Long paymentId) {

        requireLogin(user);

        // Find the payment only within the logged-in company.
        Payment payment = paymentRepository
                .findByPaymentIdAndCompanyId(
                        paymentId,
                        user.getCompanyId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found in your company."
                ));

        // Delete the payment.
        // Oracle trigger TRG_PAYMENTS_FT deletes its linked
        // financial transaction.
        // TRG_FT_BALANCE reverses the transaction's balance effect.
        paymentRepository.delete(payment);
        paymentRepository.flush();

        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // APPLY REQUEST FIELDS
    // ==========================================
    private void applyRequest(
            Payment payment,
            PaymentRequest request,
            boolean updating) {

        payment.setAccountId(request.accountId());
        payment.setPaymentType(request.paymentType());
        payment.setPartyType(request.partyType());
        payment.setPartyId(request.partyId());
        payment.setAmount(request.amount());

        if (request.paymentDate() != null) {
            payment.setPaymentDate(request.paymentDate());
        } else if (!updating) {
            payment.setPaymentDate(LocalDate.now());
        }

        payment.setPaymentMode(request.paymentMode());
        payment.setReferenceNo(request.referenceNo());
        payment.setNotes(request.notes());
    }

    // ==========================================
    // VALIDATE CASH/BANK ACCOUNT
    // ==========================================
    private void validateAccount(Long accountId, Long companyId) {

        boolean validAccount =
                accountRepository.isValidPaymentAccount(
                        accountId,
                        companyId
                );

        if (!validAccount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected account is invalid, inactive, " +
                    "or does not belong to your company."
            );
        }
    }

    // ==========================================
    // VALIDATE PAYMENT DATA
    // ==========================================
    private void validateRequest(PaymentRequest request) {

        if (!Set.of("RECEIPT", "PAYMENT")
                .contains(request.paymentType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment type must be RECEIPT or PAYMENT."
            );
        }

        if (!Set.of("CUSTOMER", "SUPPLIER")
                .contains(request.partyType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party type must be CUSTOMER or SUPPLIER."
            );
        }

        if (!Set.of("CASH", "BANK", "UPI", "CARD", "CHEQUE")
                .contains(request.paymentMode())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment mode."
            );
        }

        if ("RECEIPT".equals(request.paymentType())
                && !"CUSTOMER".equals(request.partyType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A receipt must be from a customer."
            );
        }

        if ("PAYMENT".equals(request.paymentType())
                && !"SUPPLIER".equals(request.partyType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A payment must be made to a supplier."
            );
        }
    }

    // ==========================================
    // LOGIN VALIDATION
    // ==========================================
    private void requireLogin(CompanyUserDetails user) {

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first."
            );
        }
    }

    // ==========================================
    // REQUEST DATA STRUCTURE
    // ==========================================
    public record PaymentRequest(

            @NotNull
            Long accountId,

            @NotNull
            @Pattern(regexp = "RECEIPT|PAYMENT")
            String paymentType,

            @NotNull
            @Pattern(regexp = "CUSTOMER|SUPPLIER")
            String partyType,

            @NotNull
            Long partyId,

            @NotNull
            @DecimalMin(value = "0.01")
            BigDecimal amount,

            LocalDate paymentDate,

            @NotNull
            @Pattern(regexp = "CASH|BANK|UPI|CARD|CHEQUE")
            String paymentMode,

            @Size(max = 100)
            String referenceNo,

            @Size(max = 500)
            String notes
    ) {
    }
}
