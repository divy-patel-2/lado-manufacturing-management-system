package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "USERS")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "PASSWORD_HASH")
    private String passwordHash;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "IS_ACTIVE")
    private String isActive;

    public Long getUserId() { return userId; }
    public Long getCompanyId() { return companyId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Long getRoleId() { return roleId; }
    public String getEmail() { return email; }
    public String getIsActive() { return isActive; }

    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public void setEmail(String email) { this.email = email; }
    public void setIsActive(String isActive) { this.isActive = isActive; }
}
