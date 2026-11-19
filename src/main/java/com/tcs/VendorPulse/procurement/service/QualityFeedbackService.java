package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.QualityFeedback;
import java.util.List;

public interface QualityFeedbackService {
    QualityFeedback submit(Long deliveryId, int rating, String comments, Long managerId);
    QualityFeedback get(Long id);
    List<QualityFeedback> byDelivery(Long deliveryId);
}
