package lado.com.lado_manufacturing;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;

    public SupplierController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public record SupplierRequest(
            String code,
            String name,
            String phone,
            String email,
            String address,
            String city,
            String state,
            String pincode,
            String isActive
    ) {}

    public record SupplierDto(
            Long id,
            String code,
            String name,
            String phone,
            String email,
            String address,
            String city,
            String state,
            String pincode,
            String isActive
    ) {}

    @GetMapping
    public List<SupplierDto> list(
            @AuthenticationPrincipal CompanyUserDetails user) {

        return supplierRepository
                .findByCompanyIdOrderBySupplierNameAsc(user.getCompanyId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto create(
            @RequestBody SupplierRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        if (supplierRepository.existsByCompanyIdAndSupplierCodeIgnoreCase(
                companyId,
                request.code().trim())) {

            throw new IllegalArgumentException(
                    "A supplier with this code already exists.");
        }

        Supplier supplier = new Supplier();

        supplier.setCompanyId(companyId);
        supplier.setSupplierCode(request.code().trim());
        supplier.setSupplierName(request.name().trim());
        supplier.setPhone(clean(request.phone()));
        supplier.setEmail(clean(request.email()));
        supplier.setAddress(clean(request.address()));
        supplier.setCity(clean(request.city()));
        supplier.setState(clean(request.state()));
        supplier.setPincode(clean(request.pincode()));
        supplier.setIsActive(normalizeStatus(request.isActive()));

        return toDto(supplierRepository.save(supplier));
    }

    @PutMapping("/{id}")
    public SupplierDto update(
            @PathVariable Long id,
            @RequestBody SupplierRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        Supplier supplier = supplierRepository
                .findBySupplierIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found."));

        supplierRepository
                .findByCompanyIdAndSupplierCodeIgnoreCase(
                        companyId,
                        request.code().trim())
                .filter(existing ->
                        !existing.getSupplierId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A supplier with this code already exists.");
                });

        supplier.setSupplierCode(request.code().trim());
        supplier.setSupplierName(request.name().trim());
        supplier.setPhone(clean(request.phone()));
        supplier.setEmail(clean(request.email()));
        supplier.setAddress(clean(request.address()));
        supplier.setCity(clean(request.city()));
        supplier.setState(clean(request.state()));
        supplier.setPincode(clean(request.pincode()));
        supplier.setIsActive(normalizeStatus(request.isActive()));

        return toDto(supplierRepository.save(supplier));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user) {

        Long companyId = user.getCompanyId();

        Supplier supplier = supplierRepository
                .findBySupplierIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Supplier not found."));

        try {
            supplierRepository.delete(supplier);
            supplierRepository.flush();

        } catch (DataAccessException e) {
            throw new IllegalArgumentException(
                    "This supplier is already used in purchases and cannot be deleted.");
        }
    }

    private void validate(SupplierRequest request) {

        if (request.code() == null ||
                request.code().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Supplier code is required.");
        }

        if (request.code().trim().length() > 50) {

            throw new IllegalArgumentException(
                    "Supplier code cannot exceed 50 characters.");
        }

        if (request.name() == null ||
                request.name().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Supplier name is required.");
        }

        if (request.name().trim().length() > 200) {

            throw new IllegalArgumentException(
                    "Supplier name cannot exceed 200 characters.");
        }

        if (request.phone() != null &&
                request.phone().length() > 50) {

            throw new IllegalArgumentException(
                    "Phone cannot exceed 50 characters.");
        }

        if (request.email() != null &&
                request.email().length() > 200) {

            throw new IllegalArgumentException(
                    "Email cannot exceed 200 characters.");
        }

        if (request.address() != null &&
                request.address().length() > 500) {

            throw new IllegalArgumentException(
                    "Address cannot exceed 500 characters.");
        }

        if (request.city() != null &&
                request.city().length() > 100) {

            throw new IllegalArgumentException(
                    "City cannot exceed 100 characters.");
        }

        if (request.state() != null &&
                request.state().length() > 100) {

            throw new IllegalArgumentException(
                    "State cannot exceed 100 characters.");
        }

        if (request.pincode() != null &&
                request.pincode().length() > 20) {

            throw new IllegalArgumentException(
                    "Pincode cannot exceed 20 characters.");
        }
    }

    private SupplierDto toDto(Supplier supplier) {

        return new SupplierDto(
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getSupplierName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getCity(),
                supplier.getState(),
                supplier.getPincode(),
                supplier.getIsActive()
        );
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "Y";
        }
        String status = value.trim();
        if ("Y".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)) {
            return "Y";
        }
        if ("N".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
            return "N";
        }
        throw new IllegalArgumentException("Supplier status must be Active or Inactive.");
    }
}