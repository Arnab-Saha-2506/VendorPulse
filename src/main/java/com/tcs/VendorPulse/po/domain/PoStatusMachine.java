package com.tcs.VendorPulse.po.domain;

import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PoStatusMachine {

    private static final Map<PoStatus, Set<PoStatus>> ALLOWED_TRANSITIONS = Map.of(
            PoStatus.DRAFT, EnumSet.of(PoStatus.SUBMITTED, PoStatus.CANCELLED),
            PoStatus.SUBMITTED, EnumSet.of(PoStatus.CONFIRMED, PoStatus.CANCELLED),
            PoStatus.CONFIRMED, EnumSet.of(PoStatus.IN_PROGRESS, PoStatus.CANCELLED),
            PoStatus.IN_PROGRESS, EnumSet.of(PoStatus.PARTIALLY_DELIVERED, PoStatus.DELIVERED),
            PoStatus.PARTIALLY_DELIVERED, EnumSet.of(PoStatus.PARTIALLY_DELIVERED, PoStatus.DELIVERED),
            PoStatus.DELIVERED, EnumSet.noneOf(PoStatus.class),
            PoStatus.CANCELLED, EnumSet.noneOf(PoStatus.class)
    );

    public static void validateTransition(PoStatus current, PoStatus target) {
        if (current == target) {
            return;
        }
        Set<PoStatus> validTargets = ALLOWED_TRANSITIONS.get(current);
        if (validTargets == null || !validTargets.contains(target)) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format("Cannot transition PO from %s to %s", current, target));
        }
    }
}

