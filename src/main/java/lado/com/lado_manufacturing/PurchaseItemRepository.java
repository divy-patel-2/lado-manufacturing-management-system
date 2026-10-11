package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseItemRepository
        extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByCompanyIdAndPurchaseIdOrderByPurchaseItemIdAsc(
            Long companyId,
            Long purchaseId);

    long countByCompanyIdAndPurchaseId(
            Long companyId,
            Long purchaseId);

    void deleteByCompanyIdAndPurchaseId(
            Long companyId,
            Long purchaseId);
}