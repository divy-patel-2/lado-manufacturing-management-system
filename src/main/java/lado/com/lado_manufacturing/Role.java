package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "ROLES")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "ROLE_NAME")
    private String roleName;

    public Long getRoleId() { return roleId; }
    public Long getCompanyId() { return companyId; }
    public String getRoleName() { return roleName; }
}
