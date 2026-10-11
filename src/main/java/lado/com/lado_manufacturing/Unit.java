package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "UNITS")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UNIT_ID")
    private Long unitId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "UNIT_NAME")
    private String unitName;

    @Column(name = "UNIT_SYMBOL")
    private String unitSymbol;

    public Long getUnitId() { return unitId; }
    public Long getCompanyId() { return companyId; }
    public String getUnitName() { return unitName; }
    public String getUnitSymbol() { return unitSymbol; }

    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public void setUnitSymbol(String unitSymbol) { this.unitSymbol = unitSymbol; }
}
