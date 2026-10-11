
package lado.com.lado_manufacturing;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public SalesController(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    // =========================
    // GET ALL SALES
    // =========================
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSales(
            @AuthenticationPrincipal CompanyUserDetails user
    ) {
        try {
            Long companyId = user.getCompanyId();

            List<Sale> sales =
                    saleRepository.findByCompanyIdOrderBySaleDateDescSaleIdDesc(
                            companyId
                    );

            List<SaleResponse> response = new ArrayList<>();

            for (Sale sale : sales) {
                response.add(toResponse(sale, companyId));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load sales: " + rootMessage(e)
            );
        }
    }

    // =========================
    // GET ONE SALE
    // =========================
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSale(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user
    ) {
        try {
            Long companyId = user.getCompanyId();

            Sale sale = saleRepository
                    .findBySaleIdAndCompanyId(id, companyId)
                    .orElse(null);

            if (sale == null) {
                return errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Sale not found"
                );
            }

            return ResponseEntity.ok(toResponse(sale, companyId));

        } catch (Exception e) {
            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load sale: " + rootMessage(e)
            );
        }
    }

    // =========================
    // CREATE SALE
    // =========================
    @PostMapping
    @Transactional
    public ResponseEntity<?> createSale(
            @RequestBody SaleRequest request,
            @AuthenticationPrincipal CompanyUserDetails user
    ) {
        try {
            Long companyId = user.getCompanyId();

            validateSaleRequest(request);

            String saleNo = request.saleNo.trim();

            if (saleRepository.existsByCompanyIdAndSaleNoIgnoreCase(
                    companyId, saleNo)) {
                return errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Sale number already exists"
                );
            }

            // Customer must belong to the logged-in company.
            if (customerRepository.findByCustomerIdAndCompanyId(
                    request.customerId, companyId).isEmpty()) {
                return errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Customer does not belong to this company"
                );
            }

            // Every product must belong to the logged-in company.
            validateProducts(request.items, companyId);

            Sale sale = new Sale();
            sale.setCompanyId(companyId);
            sale.setSaleNo(saleNo);
            sale.setCustomerId(request.customerId);
            sale.setSaleDate(
                    request.saleDate != null
                            ? request.saleDate
                            : LocalDate.now()
            );
            sale.setPaymentStatus(
                    normalizePaymentStatus(request.paymentStatus)
            );
            sale.setNotes(request.notes);
            sale.setTotalAmount(BigDecimal.ZERO);

            Sale savedSale = saleRepository.save(sale);

            for (SaleItemRequest itemRequest : request.items) {
                SaleItem item = new SaleItem();

                item.setCompanyId(companyId);
                item.setSaleId(savedSale.getSaleId());
                item.setProductId(itemRequest.productId);
                item.setQuantity(itemRequest.quantity);
                item.setUnitPrice(itemRequest.unitPrice);
                item.setTaxAmount(
                        itemRequest.taxAmount != null
                                ? itemRequest.taxAmount
                                : BigDecimal.ZERO
                );
                item.setDiscountAmount(
                        itemRequest.discountAmount != null
                                ? itemRequest.discountAmount
                                : BigDecimal.ZERO
                );
                item.setLineTotal(null);

                saleItemRepository.save(item);
            }

            Sale finalSale = saleRepository
                    .findBySaleIdAndCompanyId(
                            savedSale.getSaleId(), companyId)
                    .orElse(savedSale);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(finalSale, companyId));

        } catch (IllegalArgumentException e) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (DataIntegrityViolationException e) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Sale could not be saved. Check sale number, customer, product, quantity and payment status."
            );

        } catch (Exception e) {
            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create sale: " + rootMessage(e)
            );
        }
    }

    // =========================
    // UPDATE SALE
    // =========================
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateSale(
            @PathVariable Long id,
            @RequestBody SaleRequest request,
            @AuthenticationPrincipal CompanyUserDetails user
    ) {
        try {
            Long companyId = user.getCompanyId();

            validateSaleRequest(request);

            Sale sale = saleRepository
                    .findBySaleIdAndCompanyId(id, companyId)
                    .orElse(null);

            if (sale == null) {
                return errorResponse(
                        HttpStatus.NOT_FOUND, "Sale not found");
            }

            Sale duplicateSale = saleRepository
                    .findByCompanyIdAndSaleNoIgnoreCase(
                            companyId, request.saleNo.trim())
                    .orElse(null);

            if (duplicateSale != null
                    && !duplicateSale.getSaleId().equals(id)) {
                return errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Sale number already exists"
                );
            }

            if (customerRepository.findByCustomerIdAndCompanyId(
                    request.customerId, companyId).isEmpty()) {
                return errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Customer does not belong to this company"
                );
            }

            validateProducts(request.items, companyId);

            // Delete only this company's items for this sale.
            saleItemRepository.deleteByCompanyIdAndSaleId(companyId, id);

            sale.setSaleNo(request.saleNo.trim());
            sale.setCustomerId(request.customerId);
            sale.setSaleDate(
                    request.saleDate != null
                            ? request.saleDate
                            : LocalDate.now()
            );
            sale.setPaymentStatus(
                    normalizePaymentStatus(request.paymentStatus)
            );
            sale.setNotes(request.notes);

            saleRepository.save(sale);

            for (SaleItemRequest itemRequest : request.items) {
                SaleItem item = new SaleItem();

                item.setCompanyId(companyId);
                item.setSaleId(id);
                item.setProductId(itemRequest.productId);
                item.setQuantity(itemRequest.quantity);
                item.setUnitPrice(itemRequest.unitPrice);
                item.setTaxAmount(
                        itemRequest.taxAmount != null
                                ? itemRequest.taxAmount
                                : BigDecimal.ZERO
                );
                item.setDiscountAmount(
                        itemRequest.discountAmount != null
                                ? itemRequest.discountAmount
                                : BigDecimal.ZERO
                );
                item.setLineTotal(null);

                saleItemRepository.save(item);
            }

            Sale finalSale = saleRepository
                    .findBySaleIdAndCompanyId(id, companyId)
                    .orElse(sale);

            return ResponseEntity.ok(toResponse(finalSale, companyId));

        } catch (IllegalArgumentException e) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (DataIntegrityViolationException e) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Sale could not be updated. Check sale number, customer, product, quantity and payment status."
            );

        } catch (Exception e) {
            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update sale: " + rootMessage(e)
            );
        }
    }

    // =========================
    // DELETE SALE
    // =========================
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteSale(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user
    ) {
        try {
            Long companyId = user.getCompanyId();

            Sale sale = saleRepository
                    .findBySaleIdAndCompanyId(id, companyId)
                    .orElse(null);

            if (sale == null) {
                return errorResponse(
                        HttpStatus.NOT_FOUND, "Sale not found");
            }

            saleItemRepository.deleteByCompanyIdAndSaleId(companyId, id);
            saleRepository.delete(sale);

            return ResponseEntity.ok(
                    new MessageResponse("Sale deleted successfully"));

        } catch (DataIntegrityViolationException e) {
            return errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Sale cannot be deleted because it is referenced by other records."
            );

        } catch (Exception e) {
            return errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete sale: " + rootMessage(e)
            );
        }
    }

    // =========================
    // SALE VALIDATION
    // =========================
    private void validateSaleRequest(SaleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Sale data is required");
        }

        if (request.saleNo == null || request.saleNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Sale number is required");
        }

        if (request.customerId == null) {
            throw new IllegalArgumentException("Customer is required");
        }

        if (request.items == null || request.items.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one sale item is required");
        }

        String status = normalizePaymentStatus(request.paymentStatus);

        if (!status.equals("PENDING")
                && !status.equals("PARTIAL")
                && !status.equals("PAID")) {
            throw new IllegalArgumentException(
                    "Payment status must be PENDING, PARTIAL or PAID");
        }

        for (SaleItemRequest item : request.items) {
            if (item == null) {
                throw new IllegalArgumentException("Invalid sale item");
            }

            if (item.productId == null) {
                throw new IllegalArgumentException(
                        "Product is required for every sale item");
            }

            if (item.quantity == null
                    || item.quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero");
            }

            if (item.unitPrice == null
                    || item.unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Unit price cannot be negative");
            }

            if (item.taxAmount != null
                    && item.taxAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Tax amount cannot be negative");
            }

            if (item.discountAmount != null
                    && item.discountAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Discount amount cannot be negative");
            }

            BigDecimal tax = item.taxAmount != null
                    ? item.taxAmount : BigDecimal.ZERO;

            BigDecimal discount = item.discountAmount != null
                    ? item.discountAmount : BigDecimal.ZERO;

            BigDecimal calculated = item.quantity
                    .multiply(item.unitPrice)
                    .add(tax)
                    .subtract(discount);

            if (calculated.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Discount cannot make a sale item total negative");
            }
        }
    }

    // =========================
    // PRODUCT VALIDATION
    // =========================
    private void validateProducts(
            List<SaleItemRequest> items, Long companyId) {

        for (SaleItemRequest item : items) {
            if (productRepository.findByProductIdAndCompanyId(
                    item.productId, companyId).isEmpty()) {
                throw new IllegalArgumentException(
                        "Product ID " + item.productId
                                + " does not belong to this company");
            }
        }
    }

    // =========================
    // RESPONSE CONVERSION
    // =========================
    private SaleResponse toResponse(Sale sale, Long companyId) {
        List<SaleItem> items = saleItemRepository
                .findByCompanyIdAndSaleIdOrderBySaleItemIdAsc(
                        companyId, sale.getSaleId());

        SaleResponse response = new SaleResponse();
        response.id = sale.getSaleId();
        response.saleNo = sale.getSaleNo();
        response.customerId = sale.getCustomerId();
        response.saleDate = sale.getSaleDate();
        response.totalAmount = sale.getTotalAmount();
        response.paymentStatus = sale.getPaymentStatus();
        response.notes = sale.getNotes();
        response.items = new ArrayList<>();

        for (SaleItem item : items) {
            SaleItemResponse itemResponse = new SaleItemResponse();
            itemResponse.id = item.getSaleItemId();
            itemResponse.productId = item.getProductId();
            itemResponse.quantity = item.getQuantity();
            itemResponse.unitPrice = item.getUnitPrice();
            itemResponse.taxAmount = item.getTaxAmount();
            itemResponse.discountAmount = item.getDiscountAmount();
            itemResponse.lineTotal = item.getLineTotal();

            response.items.add(itemResponse);
        }

        return response;
    }

    // =========================
    // PAYMENT STATUS
    // =========================
    private String normalizePaymentStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "PENDING";
        }

        return status.trim().toUpperCase();
    }

    // =========================
    // ERROR RESPONSE
    // =========================
    private ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(message));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null
                && current.getCause() != current) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message != null
                ? message
                : current.getClass().getSimpleName();
    }

    // =========================
    // REQUEST CLASSES
    // =========================
    public static class SaleRequest {
        public String saleNo;
        public Long customerId;
        public LocalDate saleDate;
        public String paymentStatus;
        public String notes;
        public List<SaleItemRequest> items;
    }

    public static class SaleItemRequest {
        public Long productId;
        public BigDecimal quantity;
        public BigDecimal unitPrice;
        public BigDecimal taxAmount;
        public BigDecimal discountAmount;
    }

    // =========================
    // RESPONSE CLASSES
    // =========================
    public static class SaleResponse {
        public Long id;
        public String saleNo;
        public Long customerId;
        public LocalDate saleDate;
        public BigDecimal totalAmount;
        public String paymentStatus;
        public String notes;
        public List<SaleItemResponse> items;
    }

    public static class SaleItemResponse {
        public Long id;
        public Long productId;
        public BigDecimal quantity;
        public BigDecimal unitPrice;
        public BigDecimal taxAmount;
        public BigDecimal discountAmount;
        public BigDecimal lineTotal;
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }
}
