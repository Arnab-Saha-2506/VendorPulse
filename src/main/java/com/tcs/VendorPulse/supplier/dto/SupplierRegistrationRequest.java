package com.tcs.VendorPulse.supplier.dto;

import jakarta.validation.constraints.*;

public class SupplierRegistrationRequest {
    @NotBlank
    private String businessName;
    @NotBlank
    private String contactPerson;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String phone;
    @NotBlank
    private String gstin;
    @NotBlank
    private String address;
    @Size(min = 6)
    private String password;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String b) {
        this.businessName = b;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String c) {
        this.contactPerson = c;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String e) {
        this.email = e;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String p) {
        this.phone = p;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String g) {
        this.gstin = g;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String a) {
        this.address = a;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String p) {
        this.password = p;
    }
}