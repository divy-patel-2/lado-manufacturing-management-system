package lado.com.lado_manufacturing;

import jakarta.persistence.*;

@Entity
@Table(name = "SUPPLIERS")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SUPPLIER_ID")
    private Long supplierId;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "SUPPLIER_CODE")
    private String supplierCode;

    @Column(name = "SUPPLIER_NAME")
    private String supplierName;

    @Column(name = "PHONE")
    private String phone;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "CITY")
    private String city;

    @Column(name = "STATE")
    private String state;

    @Column(name = "PINCODE")
    private String pincode;

    @Column(name = "IS_ACTIVE")
    private String isActive;

    public Long getSupplierId() {
        return supplierId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}