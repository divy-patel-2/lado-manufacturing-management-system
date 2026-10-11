package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "CATEGORIES")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "CATEGORY_NAME")
    private String categoryName;

    @Column(name = "DESCRIPTION")
    private String description;

    public Long getCategoryId() { return categoryId; }
    public Long getCompanyId() { return companyId; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }

    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setDescription(String description) { this.description = description; }
}
