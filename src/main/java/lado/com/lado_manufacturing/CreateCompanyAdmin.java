
package lado.com.lado_manufacturing;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CreateCompanyAdmin {

    @Bean
    CommandLineRunner createExampleIndustriesAdmin(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Read the initial administrator password from the environment.
            String adminPassword = System.getenv("COMP02_ADMIN_PASSWORD");

            // Skip account creation unless explicitly configured.
            if (adminPassword == null || adminPassword.isBlank()) {
                System.out.println(
                    "COMP02 admin seeding skipped: password not configured."
                );
                return;
            }

            Long companyId = 21L;
            String username = "admin";

            // Do not create a duplicate administrator.
            if (userRepository
                    .findByCompanyIdAndUsername(companyId, username)
                    .isPresent()) {
                System.out.println(
                    "Example Industries admin already exists."
                );
                return;
            }

            // Confirm the company exists.
            if (companyRepository.findById(companyId).isEmpty()) {
                System.out.println(
                    "Example Industries company not found; admin not created."
                );
                return;
            }

            // Find the ADMIN role for this company.
            Role adminRole = roleRepository
                    .findByCompanyIdAndRoleName(companyId, "ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found for Example Industries."
                    ));

            AppUser user = new AppUser();

            user.setCompanyId(companyId);
            user.setUsername(username);
            user.setPasswordHash(
                passwordEncoder.encode(adminPassword)
            );
            user.setFullName("Example Industries Administrator");
            user.setRoleId(adminRole.getRoleId());
            user.setEmail("admin@example.com");
            user.setIsActive("Y");

            userRepository.save(user);

            System.out.println(
                "Example Industries administrator created successfully."
            );
        };
    }
}
