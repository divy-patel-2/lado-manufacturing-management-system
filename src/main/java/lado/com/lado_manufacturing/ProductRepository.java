package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCompanyIdOrderByProductNameAsc(Long companyId);

    Optional<Product> findByProductIdAndCompanyId(Long productId, Long companyId);

    Optional<Product> findByCompanyIdAndProductCodeIgnoreCase(
            Long companyId, String productCode);

    boolean existsByCompanyIdAndProductCodeIgnoreCase(
            Long companyId, String productCode);
}
