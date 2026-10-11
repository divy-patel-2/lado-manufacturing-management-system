package lado.com.lado_manufacturing;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;

    public ProductController(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UnitRepository unitRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
    }

    public record ProductRequest(
            String code,
            String name,
            Long categoryId,
            Long unitId,
            String description,
            BigDecimal sellingPrice,
            BigDecimal costPrice,
            BigDecimal reorderLevel
    ) {}

    public record ProductDto(
            Long id,
            String code,
            String name,
            Long categoryId,
            Long unitId,
            String description,
            BigDecimal sellingPrice,
            BigDecimal costPrice,
            BigDecimal reorderLevel,
            String isActive
    ) {}

    @GetMapping
    public List<ProductDto> list(
            @AuthenticationPrincipal CompanyUserDetails user) {

        return productRepository
                .findByCompanyIdOrderByProductNameAsc(user.getCompanyId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        if (productRepository.existsByCompanyIdAndProductCodeIgnoreCase(
                companyId, request.code().trim())) {

            throw new IllegalArgumentException(
                    "A product with this code already exists.");
        }

        validateCategoryAndUnit(
                companyId,
                request.categoryId(),
                request.unitId()
        );

        Product product = new Product();

        product.setCompanyId(companyId);
        product.setProductCode(request.code().trim());
        product.setProductName(request.name().trim());
        product.setCategoryId(request.categoryId());
        product.setUnitId(request.unitId());
        product.setDescription(clean(request.description()));
        product.setSellingPrice(request.sellingPrice());
        product.setCostPrice(request.costPrice());
        product.setReorderLevel(request.reorderLevel());
        product.setIsActive("Y");

        return toDto(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ProductDto update(
            @PathVariable Long id,
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        Product product = productRepository
                .findByProductIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found."));

        productRepository
                .findByCompanyIdAndProductCodeIgnoreCase(
                        companyId,
                        request.code().trim())
                .filter(existing ->
                        !existing.getProductId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A product with this code already exists.");
                });

        validateCategoryAndUnit(
                companyId,
                request.categoryId(),
                request.unitId()
        );

        product.setProductCode(request.code().trim());
        product.setProductName(request.name().trim());
        product.setCategoryId(request.categoryId());
        product.setUnitId(request.unitId());
        product.setDescription(clean(request.description()));
        product.setSellingPrice(request.sellingPrice());
        product.setCostPrice(request.costPrice());
        product.setReorderLevel(request.reorderLevel());

        return toDto(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user) {

        Long companyId = user.getCompanyId();

        Product product = productRepository
                .findByProductIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found."));

        try {
            productRepository.delete(product);
            productRepository.flush();

        } catch (DataAccessException e) {
            throw new IllegalArgumentException(
                    "This product is already used in transactions and cannot be deleted.");
        }
    }

    private void validate(ProductRequest request) {

        if (request.code() == null ||
                request.code().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Product code is required.");
        }

        if (request.code().trim().length() > 50) {

            throw new IllegalArgumentException(
                    "Product code cannot exceed 50 characters.");
        }

        if (request.name() == null ||
                request.name().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Product name is required.");
        }

        if (request.name().trim().length() > 200) {

            throw new IllegalArgumentException(
                    "Product name cannot exceed 200 characters.");
        }

        if (request.description() != null &&
                request.description().length() > 500) {

            throw new IllegalArgumentException(
                    "Description cannot exceed 500 characters.");
        }

        if (request.categoryId() == null) {

            throw new IllegalArgumentException(
                    "Category is required.");
        }

        if (request.unitId() == null) {

            throw new IllegalArgumentException(
                    "Unit is required.");
        }

        if (request.sellingPrice() != null &&
                request.sellingPrice().compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Selling price cannot be negative.");
        }

        if (request.costPrice() != null &&
                request.costPrice().compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Cost price cannot be negative.");
        }

        if (request.reorderLevel() != null &&
                request.reorderLevel().compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Reorder level cannot be negative.");
        }
    }

    private void validateCategoryAndUnit(
            Long companyId,
            Long categoryId,
            Long unitId) {

        categoryRepository
                .findByCategoryIdAndCompanyId(
                        categoryId,
                        companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected category is invalid."));

        unitRepository
                .findByUnitIdAndCompanyId(
                        unitId,
                        companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected unit is invalid."));
    }

    private ProductDto toDto(Product product) {

        return new ProductDto(
                product.getProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getCategoryId(),
                product.getUnitId(),
                product.getDescription(),
                product.getSellingPrice(),
                product.getCostPrice(),
                product.getReorderLevel(),
                product.getIsActive()
        );
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}