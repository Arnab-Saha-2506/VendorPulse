package com.tcs.VendorPulse.common.event;

import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.enums.PoStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class DomainEvents {

    @Data
    @Builder
    public static class PoSubmittedEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class PoCounteredEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private Short roundNo;
        private PartySide fromParty;
        private LocalDate proposedDate;
        private BigDecimal proposedPrice;
        private String reason;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class PoAgreedEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private LocalDate agreedDeliveryDate;
        private BigDecimal agreedPriceTotal;
        private UUID acceptedByManufacturer;
        private UUID acceptedBySupplier;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class PoRevisedEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private Short seqNo;
        private LocalDate oldDate;
        private LocalDate newDate;
        private String reason;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class PoDispatchedEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private LocalDate dispatchedOn;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class PoDeliveredEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private LocalDate deliveryDate;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class PoCancelledEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private String cancelReason;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    public static class RegistrationDecidedEvent {
        private UUID supplierId;
        private String companyName;
        private boolean approved;
        private String rejectionReason;
        private UUID decidedBy;
        private String decidedByName;
    }

    @Data
    @Builder
    public static class DeliveryRecordedEvent {
        private UUID poId;
        private String poNumber;
        private UUID supplierId;
        private UUID deliveryId;
        private BigDecimal deliveredQty;
        private BigDecimal acceptedQty;
        private BigDecimal rejectedQty;
        private Short qualityRating;
        private PoStatus newPoStatus;
        private UUID userId;
        private String userName;
    }
}

