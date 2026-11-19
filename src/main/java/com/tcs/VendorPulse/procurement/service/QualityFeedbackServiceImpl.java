package com.tcs.VendorPulse.procurement.service;

import com.tcs.VendorPulse.procurement.entity.*;
import com.tcs.VendorPulse.procurement.repository.*;
import com.tcs.VendorPulse.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class QualityFeedbackServiceImpl implements QualityFeedbackService {
    private final QualityFeedbackRepository feedbacks;
    private final DeliveryRepository deliveries;
    private final UserRepository users;

    public QualityFeedbackServiceImpl(QualityFeedbackRepository f, DeliveryRepository d, UserRepository u) {
        feedbacks = f; deliveries = d; users = u;
    }

    @Transactional
    public QualityFeedback submit(Long deliveryId, int rating, String comments, Long managerId) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("rating must be 1-5");
        Delivery del = deliveries.findById(deliveryId).orElseThrow();
        QualityFeedback qf = new QualityFeedback();
        qf.setDelivery(del); qf.setRating(rating); qf.setComments(comments);
        qf.setRecordedBy(users.findById(managerId).orElseThrow());
        return feedbacks.save(qf);
    }

    public QualityFeedback get(Long id) { return feedbacks.findById(id).orElseThrow(); }
    public List<QualityFeedback> byDelivery(Long deliveryId) { return feedbacks.findAll().stream().filter(q -> q.getDelivery().getId().equals(deliveryId)).toList(); }
}
