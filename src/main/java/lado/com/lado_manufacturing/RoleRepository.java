package lado.com.lado_manufacturing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCompanyIdAndRoleName(Long companyId, String roleName);
}
