package com.tcs.VendorPulse.admin.service;

import com.tcs.VendorPulse.supplier.entity.Supplier;
import java.util.List;

public interface AdminService {
    List<Supplier> pendingSuppliers();

    Supplier getSupplier(Long id);

    Supplier approve(Long id, Long adminId, String remarks);

    Supplier reject(Long id, Long adminId, String remarks);
}