package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "PRODUCTS")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "PRODUCT_CODE")
    private String productCode;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "UNIT_ID")
    private Long unitId;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SELLING_PRICE")
    private java.math.BigDecimal sellingPrice;

    @Column(name = "COST_PRICE")
    private java.math.BigDecimal costPrice;

    @Column(name = "REORDER_LEVEL")
    private java.math.BigDecimal reorderLevel;

    @Column(name = "IS_ACTIVE")
    private String isActive;

    public Long getProductId() {
        return productId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public String getDescription() {
        return description;
    }

    public java.math.BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public java.math.BigDecimal getCostPrice() {
        return costPrice;
    }

    public java.math.BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSellingPrice(java.math.BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setCostPrice(java.math.BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setReorderLevel(java.math.BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}