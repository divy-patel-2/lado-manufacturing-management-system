package lado.com.lado_manufacturing;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PURCHASE_ITEMS")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_ITEM_ID")
    private Long purchaseItemId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "PURCHASE_ID")
    private Long purchaseId;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "UNIT_PRICE")
    private BigDecimal unitPrice;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "DISCOUNT_AMOUNT")
    private BigDecimal discountAmount;

    @Column(name = "LINE_TOTAL")
    private BigDecimal lineTotal;

    public Long getPurchaseItemId() {
        return purchaseItemId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public Long getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}