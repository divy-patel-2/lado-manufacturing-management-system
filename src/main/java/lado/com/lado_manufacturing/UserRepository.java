package lado.com.lado_manufacturing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByCompanyIdAndUsername(Long companyId, String username);
    long countByCompanyId(Long companyId);
}
