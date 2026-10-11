package lado.com.lado_manufacturing;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public record CustomerRequest(
            String code,
            String name,
            String phone,
            String email,
            String address,
            String city,
            String state,
            String pincode
    ) {}

    public record CustomerDto(
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
    public List<CustomerDto> list(
            @AuthenticationPrincipal CompanyUserDetails user) {

        return customerRepository
                .findByCompanyIdOrderByCustomerNameAsc(user.getCompanyId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(
            @RequestBody CustomerRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        if (customerRepository
                .existsByCompanyIdAndCustomerCodeIgnoreCase(
                        companyId,
                        request.code().trim())) {

            throw new IllegalArgumentException(
                    "A customer with this code already exists.");
        }

        Customer customer = new Customer();

        customer.setCompanyId(companyId);
        customer.setCustomerCode(request.code().trim());
        customer.setCustomerName(request.name().trim());
        customer.setPhone(clean(request.phone()));
        customer.setEmail(clean(request.email()));
        customer.setAddress(clean(request.address()));
        customer.setCity(clean(request.city()));
        customer.setState(clean(request.state()));
        customer.setPincode(clean(request.pincode()));
        customer.setIsActive("Y");

        return toDto(customerRepository.save(customer));
    }

    @PutMapping("/{id}")
    public CustomerDto update(
            @PathVariable Long id,
            @RequestBody CustomerRequest request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        validate(request);

        Long companyId = user.getCompanyId();

        Customer customer = customerRepository
                .findByCustomerIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found."));

        customerRepository
                .findByCompanyIdAndCustomerCodeIgnoreCase(
                        companyId,
                        request.code().trim())
                .filter(existing ->
                        !existing.getCustomerId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A customer with this code already exists.");
                });

        customer.setCustomerCode(request.code().trim());
        customer.setCustomerName(request.name().trim());
        customer.setPhone(clean(request.phone()));
        customer.setEmail(clean(request.email()));
        customer.setAddress(clean(request.address()));
        customer.setCity(clean(request.city()));
        customer.setState(clean(request.state()));
        customer.setPincode(clean(request.pincode()));

        return toDto(customerRepository.save(customer));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CompanyUserDetails user) {

        Long companyId = user.getCompanyId();

        Customer customer = customerRepository
                .findByCustomerIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found."));

        try {
            customerRepository.delete(customer);
            customerRepository.flush();

        } catch (DataAccessException e) {
            throw new IllegalArgumentException(
                    "This customer is already used in sales and cannot be deleted.");
        }
    }

    private void validate(CustomerRequest request) {

        if (request.code() == null ||
                request.code().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Customer code is required.");
        }

        if (request.code().trim().length() > 50) {

            throw new IllegalArgumentException(
                    "Customer code cannot exceed 50 characters.");
        }

        if (request.name() == null ||
                request.name().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Customer name is required.");
        }

        if (request.name().trim().length() > 200) {

            throw new IllegalArgumentException(
                    "Customer name cannot exceed 200 characters.");
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

    private CustomerDto toDto(Customer customer) {

        return new CustomerDto(
                customer.getCustomerId(),
                customer.getCustomerCode(),
                customer.getCustomerName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getPincode(),
                customer.getIsActive()
        );
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}