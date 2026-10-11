package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "COMPANIES")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "COMPANY_CODE")
    private String companyCode;

    @Column(name = "COMPANY_NAME")
    private String companyName;

    @Column(name = "IS_ACTIVE")
    private String isActive;

    public Long getCompanyId() { return companyId; }
    public String getCompanyCode() { return companyCode; }
    public String getCompanyName() { return companyName; }
    public String getIsActive() { return isActive; }
}
