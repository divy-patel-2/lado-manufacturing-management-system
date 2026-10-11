package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByCompanyIdOrderByPurchaseDateDescPurchaseIdDesc(
            Long companyId);

    Optional<Purchase> findByPurchaseIdAndCompanyId(
            Long purchaseId,
            Long companyId);

    Optional<Purchase> findByCompanyIdAndPurchaseNoIgnoreCase(
            Long companyId,
            String purchaseNo);

    boolean existsByCompanyIdAndPurchaseNoIgnoreCase(
            Long companyId,
            String purchaseNo);
}