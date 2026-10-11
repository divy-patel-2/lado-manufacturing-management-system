
package lado.com.lado_manufacturing;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PAYMENTS")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    @Column(name = "COMPANY_ID", nullable = false)
    private Long companyId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "PAYMENT_TYPE", nullable = false, length = 20)
    private String paymentType;

    @Column(name = "PARTY_TYPE", nullable = false, length = 20)
    private String partyType;

    @Column(name = "PARTY_ID", nullable = false)
    private Long partyId;

    @Column(name = "AMOUNT", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "PAYMENT_DATE")
    private LocalDate paymentDate;

    @Column(name = "PAYMENT_MODE", nullable = false, length = 20)
    private String paymentMode;

    @Column(name = "REFERENCE_NO", length = 100)
    private String referenceNo;

    @Column(name = "NOTES", length = 500)
    private String notes;

    public Payment() {
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getPartyType() {
        return partyType;
    }

    public Long getPartyId() {
        return partyId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public void setPartyType(String partyType) {
        this.partyType = partyType;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}