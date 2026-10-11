package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCompanyIdOrderByCustomerNameAsc(Long companyId);

    Optional<Customer> findByCustomerIdAndCompanyId(
            Long customerId,
            Long companyId);

    Optional<Customer> findByCompanyIdAndCustomerCodeIgnoreCase(
            Long companyId,
            String customerCode);

    boolean existsByCompanyIdAndCustomerCodeIgnoreCase(
            Long companyId,
            String customerCode);
}
