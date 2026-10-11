package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByCompanyIdOrderBySupplierNameAsc(Long companyId);

    Optional<Supplier> findBySupplierIdAndCompanyId(
            Long supplierId,
            Long companyId);

    Optional<Supplier> findByCompanyIdAndSupplierCodeIgnoreCase(
            Long companyId,
            String supplierCode);

    boolean existsByCompanyIdAndSupplierCodeIgnoreCase(
            Long companyId,
            String supplierCode);
}