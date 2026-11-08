package com.tcs.VendorPulse.scoring.service;

import com.tcs.VendorPulse.scoring.entity.SupplierScore;

public interface SupplierScoringService {
    SupplierScore calculate(Long supplierId);

    SupplierScore get(Long supplierId);
}