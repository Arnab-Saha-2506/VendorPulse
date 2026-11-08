package com.tcs.VendorPulse.procurement.entity;

import com.tcs.VendorPulse.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quality_feedback")
public class QualityFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;
    @Column(nullable = false)
    private int rating; // 1-5
    @Column(columnDefinition = "TEXT")
    private String comments;
    @ManyToOne(optional = false)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        recordedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery d) {
        this.delivery = d;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int r) {
        this.rating = r;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String c) {
        this.comments = c;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User u) {
        this.recordedBy = u;
    }
}