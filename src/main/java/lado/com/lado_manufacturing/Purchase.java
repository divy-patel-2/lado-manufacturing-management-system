package lado.com.lado_manufacturing;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PURCHASES")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_ID")
    private Long purchaseId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "PURCHASE_NO")
    private String purchaseNo;

    @Column(name = "SUPPLIER_ID")
    private Long supplierId;

    @Column(name = "PURCHASE_DATE")
    private LocalDate purchaseDate;

    @Column(name = "TOTAL_AMOUNT")
    private BigDecimal totalAmount;

    @Column(name = "PAYMENT_STATUS")
    private String paymentStatus;

    @Column(name = "NOTES")
    private String notes;

    public Long getPurchaseId() {
        return purchaseId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getPurchaseNo() {
        return purchaseNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setPurchaseNo(String purchaseNo) {
        this.purchaseNo = purchaseNo;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}