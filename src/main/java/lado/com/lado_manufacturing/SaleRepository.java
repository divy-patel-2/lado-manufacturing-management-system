package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByCompanyIdOrderBySaleDateDescSaleIdDesc(Long companyId);

    Optional<Sale> findBySaleIdAndCompanyId(Long saleId, Long companyId);

    Optional<Sale> findByCompanyIdAndSaleNoIgnoreCase(
            Long companyId,
            String saleNo
    );

    boolean existsByCompanyIdAndSaleNoIgnoreCase(
            Long companyId,
            String saleNo
    );
}