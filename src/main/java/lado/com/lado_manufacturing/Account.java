package lado.com.lado_manufacturing;

import java.math.BigDecimal;

public class Account {

    private Long accountId;
    private Long companyId;
    private String accountName;
    private String accountType;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private String isActive;

    public Account() {
    }

    public Account(
            Long accountId,
            Long companyId,
            String accountName,
            String accountType,
            BigDecimal openingBalance,
            BigDecimal currentBalance,
            String isActive) {

        this.accountId = accountId;
        this.companyId = companyId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
        this.currentBalance = currentBalance;
        this.isActive = isActive;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}