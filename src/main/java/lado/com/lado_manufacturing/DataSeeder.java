
package lado.com.lado_manufacturing;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            CompanyRepository companyRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Read the initial administrator password from the environment.
        String adminPassword = System.getenv("LADO01_ADMIN_PASSWORD");

        // Skip account creation unless explicitly configured.
        if (adminPassword == null || adminPassword.isBlank()) {
            System.out.println(
                "LADO01 admin seeding skipped: password not configured."
            );
            return;
        }

        companyRepository.findByCompanyCode("LADO01").ifPresent(company -> {
            // Do not create another user if this company already has users.
            if (userRepository.countByCompanyId(company.getCompanyId()) > 0) {
                return;
            }

            roleRepository.findByCompanyIdAndRoleName(
                    company.getCompanyId(), "ADMIN")
                .ifPresent(role -> {
                    AppUser user = new AppUser();

                    user.setCompanyId(company.getCompanyId());
                    user.setUsername("admin");
                    user.setPasswordHash(
                        passwordEncoder.encode(adminPassword)
                    );
                    user.setFullName("Administrator");
                    user.setRoleId(role.getRoleId());
                    user.setIsActive("Y");

                    userRepository.save(user);

                    System.out.println(
                        "Created initial administrator for LADO01."
                    );
                });
        });
    }
}
