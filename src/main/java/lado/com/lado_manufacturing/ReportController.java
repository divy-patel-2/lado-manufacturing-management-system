package lado.com.lado_manufacturing;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")

public class ReportController {

   private final SaleRepository saleRepository;
private final ReportRepository reportRepository;

public ReportController(
        SaleRepository saleRepository,
        ReportRepository reportRepository) {

    this.saleRepository = saleRepository;
    this.reportRepository = reportRepository;
}

    // SALES REPORT
    @GetMapping("/sales")
    public ResponseEntity<?> getSalesReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @AuthenticationPrincipal CompanyUserDetails user
    ) {
        try {
            if (user == null || user.getCompanyId() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please log in to view reports."));
            }

            if (fromDate != null && toDate != null
                    && fromDate.isAfter(toDate)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "From date cannot be after to date."));
            }

            Long companyId = user.getCompanyId();

            List<Sale> allSales =
                    saleRepository.findByCompanyIdOrderBySaleDateDescSaleIdDesc(
                            companyId
                    );

            List<Map<String, Object>> rows = new ArrayList<>();

            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;
            BigDecimal partialAmount = BigDecimal.ZERO;
            BigDecimal pendingAmount = BigDecimal.ZERO;

            int paidCount = 0;
            int partialCount = 0;
            int pendingCount = 0;

            for (Sale sale : allSales) {
                LocalDate saleDate = sale.getSaleDate();

                if (saleDate == null) {
                    continue;
                }

                if (fromDate != null && saleDate.isBefore(fromDate)) {
                    continue;
                }

                if (toDate != null && saleDate.isAfter(toDate)) {
                    continue;
                }

                BigDecimal amount = sale.getTotalAmount() != null
                        ? sale.getTotalAmount()
                        : BigDecimal.ZERO;

                String status = sale.getPaymentStatus() != null
                        ? sale.getPaymentStatus().trim().toUpperCase()
                        : "PENDING";

                totalSales = totalSales.add(amount);

                switch (status) {
                    case "PAID":
                        paidAmount = paidAmount.add(amount);
                        paidCount++;
                        break;
                    case "PARTIAL":
                        partialAmount = partialAmount.add(amount);
                        partialCount++;
                        break;
                    default:
                        pendingAmount = pendingAmount.add(amount);
                        pendingCount++;
                        break;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("saleId", sale.getSaleId());
                row.put("saleNo", sale.getSaleNo());
                row.put("customerId", sale.getCustomerId());
                row.put("saleDate", saleDate);
                row.put("totalAmount", amount);
                row.put("paymentStatus", status);
                rows.add(row);
            }

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("fromDate", fromDate);
            report.put("toDate", toDate);
            report.put("totalSalesCount", rows.size());
            report.put("totalSalesAmount", totalSales);
            report.put("paidCount", paidCount);
            report.put("paidAmount", paidAmount);
            report.put("partialCount", partialCount);
            report.put("partialAmount", partialAmount);
            report.put("pendingCount", pendingCount);
            report.put("pendingAmount", pendingAmount);
            report.put("sales", rows);

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error",
                            "Unable to generate sales report."
                    ));
        }
    }
    // INVENTORY REPORT
@GetMapping("/inventory")
public ResponseEntity<?> getInventoryReport(
        @AuthenticationPrincipal CompanyUserDetails user) {

    try {
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Please log in to view reports."
                    ));
        }

        Long companyId = user.getCompanyId();

        List<Map<String, Object>> inventory =
                reportRepository.getInventoryReport(companyId);

        Map<String, Object> report = new LinkedHashMap<>();

        report.put("totalProducts", inventory.size());

        long lowStockCount = inventory.stream()
                .filter(row ->
                        "LOW_STOCK".equals(
                                String.valueOf(row.get("STOCK_STATUS"))
                        ))
                .count();

        long outOfStockCount = inventory.stream()
                .filter(row ->
                        "OUT_OF_STOCK".equals(
                                String.valueOf(row.get("STOCK_STATUS"))
                        ))
                .count();

        report.put("lowStockCount", lowStockCount);
        report.put("outOfStockCount", outOfStockCount);
        report.put("inventory", inventory);

        return ResponseEntity.ok(report);

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error",
                        "Unable to generate inventory report."
                ));
    }
}

    // =========================================
    // PURCHASE REPORT
    // =========================================

    @GetMapping("/purchases")
    public ResponseEntity<?> getPurchaseReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @AuthenticationPrincipal CompanyUserDetails user) {

        try {
            if (user == null || user.getCompanyId() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error",
                                "Please log in to view reports."
                        ));
            }

            if (fromDate != null && toDate != null
                    && fromDate.isAfter(toDate)) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "From date cannot be after to date."
                        ));
            }

            List<Map<String, Object>> purchases =
                    reportRepository.getPurchaseReport(
                            user.getCompanyId(),
                            fromDate,
                            toDate
                    );

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;
            BigDecimal partialAmount = BigDecimal.ZERO;
            BigDecimal pendingAmount = BigDecimal.ZERO;

            int paidCount = 0;
            int partialCount = 0;
            int pendingCount = 0;

            for (Map<String, Object> purchase : purchases) {

                Object rawAmount = purchase.get("TOTAL_AMOUNT");

                BigDecimal amount = rawAmount == null
                        ? BigDecimal.ZERO
                        : new BigDecimal(rawAmount.toString());

                totalAmount = totalAmount.add(amount);

                String status = purchase.get("PAYMENT_STATUS") == null
                        ? "PENDING"
                        : purchase.get("PAYMENT_STATUS")
                                .toString()
                                .trim()
                                .toUpperCase();

                switch (status) {
                    case "PAID":
                        paidAmount = paidAmount.add(amount);
                        paidCount++;
                        break;

                    case "PARTIAL":
                        partialAmount = partialAmount.add(amount);
                        partialCount++;
                        break;

                    default:
                        pendingAmount = pendingAmount.add(amount);
                        pendingCount++;
                        break;
                }
            }

            Map<String, Object> report = new LinkedHashMap<>();

            report.put("fromDate", fromDate);
            report.put("toDate", toDate);
            report.put("totalPurchasesCount", purchases.size());
            report.put("totalPurchasesAmount", totalAmount);

            report.put("paidCount", paidCount);
            report.put("paidAmount", paidAmount);

            report.put("partialCount", partialCount);
            report.put("partialAmount", partialAmount);

            report.put("pendingCount", pendingCount);
            report.put("pendingAmount", pendingAmount);

            report.put("purchases", purchases);

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(Map.of(
                    "error",
                    "Unable to generate purchase report."
            ));
        }
    }

@GetMapping("/payments")
public ResponseEntity<?> getPaymentReport(
        @AuthenticationPrincipal CompanyUserDetails user,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate) {

    if (user == null || user.getCompanyId() == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Please log in first."));
    }

    if (fromDate != null && toDate != null
            && fromDate.isAfter(toDate)) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        "From date cannot be after to date."
                ));
    }

    try {
        List<Map<String, Object>> payments =
                reportRepository.getPaymentReport(
                        user.getCompanyId(), fromDate, toDate);

        BigDecimal totalReceipts = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        int receiptCount = 0;
        int paymentCount = 0;

        for (Map<String, Object> payment : payments) {
            String type = String.valueOf(
                    payment.get("PAYMENT_TYPE")
            ).trim().toUpperCase();

            Object amountValue = payment.get("AMOUNT");
            BigDecimal amount = amountValue == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(amountValue.toString());

            if ("RECEIPT".equals(type)) {
                totalReceipts = totalReceipts.add(amount);
                receiptCount++;
            } else if ("PAYMENT".equals(type)) {
                totalPayments = totalPayments.add(amount);
                paymentCount++;
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fromDate", fromDate);
        response.put("toDate", toDate);
        response.put("totalPaymentRecords", payments.size());
        response.put("totalReceipts", totalReceipts);
        response.put("receiptCount", receiptCount);
        response.put("totalPayments", totalPayments);
        response.put("paymentCount", paymentCount);
        response.put(
                "netCashFlow",
                totalReceipts.subtract(totalPayments)
        );
        response.put("payments", payments);

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        e.printStackTrace();

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR
        ).body(Map.of(
                "error",
                "Unable to generate payment report."
        ));
    }
}
}