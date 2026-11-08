package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.Delivery;

import java.time.LocalDate;

public interface DeliveryService {
    Delivery record(Long poId, LocalDate actualDate, int delivered, int accepted, int rejected, String remarks, Long managerId);
}
