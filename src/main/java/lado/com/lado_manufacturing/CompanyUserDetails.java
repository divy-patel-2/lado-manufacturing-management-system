package lado.com.lado_manufacturing;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The logged-in user. Carries the company id so every query can be scoped to it. */
public class CompanyUserDetails implements UserDetails {

    private final Long userId;
    private final Long companyId;
    private final String companyName;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String roleName;
    private final boolean enabled;

    public CompanyUserDetails(Long userId, Long companyId, String companyName,
                              String username, String passwordHash, String fullName,
                              String roleName, boolean enabled) {
        this.userId = userId;
        this.companyId = companyId;
        this.companyName = companyName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleName = roleName;
        this.enabled = enabled;
    }

    public Long getUserId() { return userId; }
    public Long getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public String getFullName() { return fullName; }
    public String getRoleName() { return roleName; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return enabled; }
}
