package lado.com.lado_manufacturing;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCompanyIdOrderByPaymentDateDescPaymentIdDesc(
            Long companyId
    );

    Optional<Payment> findByPaymentIdAndCompanyId(
            Long paymentId,
            Long companyId
    );

}
