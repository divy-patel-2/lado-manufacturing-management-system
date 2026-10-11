package lado.com.lado_manufacturing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByCompanyIdOrderByUnitNameAsc(Long companyId);
    Optional<Unit> findByUnitIdAndCompanyId(Long unitId, Long companyId);
    Optional<Unit> findByCompanyIdAndUnitNameIgnoreCase(Long companyId, String unitName);
    boolean existsByCompanyIdAndUnitNameIgnoreCase(Long companyId, String unitName);
}
