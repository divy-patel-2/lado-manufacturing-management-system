package lado.com.lado_manufacturing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findByCompanyIdAndSaleIdOrderBySaleItemIdAsc(
            Long companyId,
            Long saleId
    );

    long countByCompanyIdAndSaleId(
            Long companyId,
            Long saleId
    );

    void deleteByCompanyIdAndSaleId(
            Long companyId,
            Long saleId
    );
}