package com.tcs.VendorPulse.dashboard.dto;

import com.tcs.VendorPulse.alert.dto.AlertDto;
import com.tcs.VendorPulse.po.dto.PoDtos.PoSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PmDashboardDto {
        private long openPos;
        private long pendingApprovals;
        private long highCdiSuppliers;
        private long activeAlerts;
        private List<PoSummaryDto> posNeedingAttention;
        private List<AlertDto> alerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDashboardDto {
        private long proposalsAwaiting;
        private long openOrders;
        private BigDecimal reliabilityScore;
        private BigDecimal cdi;
        private List<PoSummaryDto> needsAction;
    }
}

