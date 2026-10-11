package lado.com.lado_manufacturing;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public PurchaseController(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository) {

        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    public record PurchaseItemRequest(
            Long productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxAmount,
            BigDecimal discountAmount
    ) {}

    public record PurchaseRequest(
            String purchaseNo,
            Long supplierId,
            LocalDate purchaseDate,
            String paymentStatus,
            String notes,
            List<PurchaseItemRequest> items
    ) {}

    public record PurchaseDto(
            Long id,
            String purchaseNo,
            Long supplierId,
            LocalDate purchaseDate,
            BigDecimal totalAmount,
            String paymentStatus,
            String notes,
            List<PurchaseItemDto> items
    ) {}

    public record PurchaseItemDto(
            Long id,
            Long productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal lineTotal
    ) {}

    @GetMapping
    public List<PurchaseDto> list(
            @AuthenticationPrincipal CompanyUserDetails user) {

        Long companyId = user.getCompanyId();

        return purchaseRepository
                .findByCompanyIdOrderByPurchaseDateDescPurchaseIdDesc(companyId)
                .stream()
                .map(p -> toDto(p, companyId))
                .toList();
    }

    @GetMapping("/{id}")
    public PurchaseDto get(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user) {

        Long companyId = user.getCompanyId();

        Purchase purchase = purchaseRepository
                .findByPurchaseIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Purchase not found."));

        return toDto(purchase, companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PurchaseDto create(
            @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        if (purchaseRepository.existsByCompanyIdAndPurchaseNoIgnoreCase(
                companyId,
                request.purchaseNo().trim())) {

            throw new IllegalArgumentException(
                    "A purchase with this number already exists.");
        }

        validateSupplier(request.supplierId(), companyId);

        Purchase purchase = new Purchase();

        purchase.setCompanyId(companyId);
        purchase.setPurchaseNo(request.purchaseNo().trim());
        purchase.setSupplierId(request.supplierId());

        purchase.setPurchaseDate(
                request.purchaseDate() == null
                        ? LocalDate.now()
                        : request.purchaseDate());

        purchase.setPaymentStatus(
                clean(request.paymentStatus()) == null
                        ? "PENDING"
                        : request.paymentStatus().trim().toUpperCase());

        purchase.setNotes(clean(request.notes()));
        purchase.setTotalAmount(BigDecimal.ZERO);

        Purchase saved = purchaseRepository.save(purchase);

        BigDecimal total =
                saveItems(saved, request.items(), companyId);

        saved.setTotalAmount(total);

        return toDto(
                purchaseRepository.save(saved),
                companyId);
    }

    @PutMapping("/{id}")
    @Transactional
    public PurchaseDto update(
            @PathVariable Long id,
            @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        Purchase purchase = purchaseRepository
                .findByPurchaseIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Purchase not found."));

        purchaseRepository
                .findByCompanyIdAndPurchaseNoIgnoreCase(
                        companyId,
                        request.purchaseNo().trim())
                .filter(existing ->
                        !existing.getPurchaseId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A purchase with this number already exists.");
                });

        validateSupplier(request.supplierId(), companyId);

        purchase.setPurchaseNo(request.purchaseNo().trim());
        purchase.setSupplierId(request.supplierId());

        purchase.setPurchaseDate(
                request.purchaseDate() == null
                        ? LocalDate.now()
                        : request.purchaseDate());

        purchase.setPaymentStatus(
                clean(request.paymentStatus()) == null
                        ? "PENDING"
                        : request.paymentStatus().trim().toUpperCase());

        purchase.setNotes(clean(request.notes()));

        purchaseItemRepository.deleteByCompanyIdAndPurchaseId(
                companyId,
                id);

        BigDecimal total =
                saveItems(purchase, request.items(), companyId);

        purchase.setTotalAmount(total);

        return toDto(
                purchaseRepository.save(purchase),
                companyId);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user) {

        Long companyId = user.getCompanyId();

        Purchase purchase = purchaseRepository
                .findByPurchaseIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Purchase not found."));

        purchaseItemRepository.deleteByCompanyIdAndPurchaseId(
                companyId,
                id);

        purchaseRepository.delete(purchase);
    }

    private BigDecimal saveItems(
            Purchase purchase,
            List<PurchaseItemRequest> items,
            Long companyId) {

        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseItemRequest request : items) {

            validateItem(request);

            productRepository
                    .findByProductIdAndCompanyId(
                            request.productId(),
                            companyId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found."));

            BigDecimal tax = nz(request.taxAmount());
            BigDecimal discount = nz(request.discountAmount());

            BigDecimal lineTotal =
                    request.quantity()
                            .multiply(request.unitPrice())
                            .add(tax)
                            .subtract(discount);

            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Line total cannot be negative.");
            }

            PurchaseItem item = new PurchaseItem();

            item.setCompanyId(companyId);
            item.setPurchaseId(purchase.getPurchaseId());
            item.setProductId(request.productId());
            item.setQuantity(request.quantity());
            item.setUnitPrice(request.unitPrice());
            item.setTaxAmount(tax);
            item.setDiscountAmount(discount);
            item.setLineTotal(lineTotal);

            purchaseItemRepository.save(item);

            total = total.add(lineTotal);
        }

        return total;
    }

    private void validate(PurchaseRequest request) {

        if (request.purchaseNo() == null ||
                request.purchaseNo().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Purchase number is required.");
        }

        if (request.purchaseNo().trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Purchase number cannot exceed 50 characters.");
        }

        if (request.supplierId() == null) {
            throw new IllegalArgumentException(
                    "Supplier is required.");
        }

        if (request.items() == null ||
                request.items().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one purchase item is required.");
        }
    }

    private void validateItem(PurchaseItemRequest request) {

        if (request.productId() == null) {
            throw new IllegalArgumentException(
                    "Product is required for every purchase item.");
        }

        if (request.quantity() == null ||
                request.quantity().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        if (request.unitPrice() == null ||
                request.unitPrice().compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Unit price cannot be negative.");
        }

        if (request.taxAmount() != null &&
                request.taxAmount().compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Tax amount cannot be negative.");
        }

        if (request.discountAmount() != null &&
                request.discountAmount().compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Discount amount cannot be negative.");
        }
    }

    private void validateSupplier(
            Long supplierId,
            Long companyId) {

        supplierRepository
                .findBySupplierIdAndCompanyId(
                        supplierId,
                        companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found."));
    }

    private PurchaseDto toDto(
            Purchase purchase,
            Long companyId) {

        List<PurchaseItemDto> items =
                purchaseItemRepository
                        .findByCompanyIdAndPurchaseIdOrderByPurchaseItemIdAsc(
                                companyId,
                                purchase.getPurchaseId())
                        .stream()
                        .map(item -> new PurchaseItemDto(
                                item.getPurchaseItemId(),
                                item.getProductId(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getTaxAmount(),
                                item.getDiscountAmount(),
                                item.getLineTotal()))
                        .toList();

        return new PurchaseDto(
                purchase.getPurchaseId(),
                purchase.getPurchaseNo(),
                purchase.getSupplierId(),
                purchase.getPurchaseDate(),
                purchase.getTotalAmount(),
                purchase.getPaymentStatus(),
                purchase.getNotes(),
                items);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty()
                ? null
                : value.trim();
    }
}