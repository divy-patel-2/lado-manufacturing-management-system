package lado.com.lado_manufacturing;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * The login form sends "COMPANYCODE:username" as the username, so the same
 * username can exist in different companies.
 */
@Service
public class CompanyUserDetailsService implements UserDetailsService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CompanyUserDetailsService(CompanyRepository companyRepository,
                                     UserRepository userRepository,
                                     RoleRepository roleRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        int idx = input == null ? -1 : input.indexOf(':');
        if (idx <= 0) {
            throw new UsernameNotFoundException("Company and username are required");
        }
        String companyCode = input.substring(0, idx).trim().toUpperCase();
        String username = input.substring(idx + 1).trim();

        Company company = companyRepository.findByCompanyCode(companyCode)
                .filter(c -> "Y".equals(c.getIsActive()))
                .orElseThrow(() -> new UsernameNotFoundException("Unknown company"));

        AppUser user = userRepository.findByCompanyIdAndUsername(company.getCompanyId(), username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new UsernameNotFoundException("User has no role"));

        return new CompanyUserDetails(
                user.getUserId(), company.getCompanyId(), company.getCompanyName(),
                user.getUsername(), user.getPasswordHash(), user.getFullName(),
                role.getRoleName(), "Y".equals(user.getIsActive()));
    }
}
