package com.tcs.VendorPulse.supplier.service;

import com.tcs.VendorPulse.supplier.dto.SupplierRegistrationRequest;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.entity.SupplierDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SupplierService {
    Supplier register(SupplierRegistrationRequest req, MultipartFile file) throws IOException;
    SupplierDocument getSupplierCertificate(Long supplierId);
}
