package lado.com.lado_manufacturing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByCompanyIdOrderByCategoryNameAsc(Long companyId);
    Optional<Category> findByCategoryIdAndCompanyId(Long categoryId, Long companyId);
    Optional<Category> findByCompanyIdAndCategoryNameIgnoreCase(Long companyId, String categoryName);
    boolean existsByCompanyIdAndCategoryNameIgnoreCase(Long companyId, String categoryName);
}
