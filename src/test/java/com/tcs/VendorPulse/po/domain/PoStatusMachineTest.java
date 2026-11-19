package com.tcs.VendorPulse.po.domain;

import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoStatusMachineTest {

    @Test
    void testValidTransitions() {
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.DRAFT, PoStatus.SUBMITTED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.DRAFT, PoStatus.CANCELLED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.SUBMITTED, PoStatus.CONFIRMED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.SUBMITTED, PoStatus.CANCELLED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.CONFIRMED, PoStatus.IN_PROGRESS));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.CONFIRMED, PoStatus.CANCELLED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.IN_PROGRESS, PoStatus.PARTIALLY_DELIVERED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.IN_PROGRESS, PoStatus.DELIVERED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.PARTIALLY_DELIVERED, PoStatus.DELIVERED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.PARTIALLY_DELIVERED, PoStatus.PARTIALLY_DELIVERED));
        assertDoesNotThrow(() -> PoStatusMachine.validateTransition(PoStatus.DRAFT, PoStatus.DRAFT));
    }

    @Test
    void testTerminalStateDeliveredCannotTransition() {
        ApiException ex = assertThrows(ApiException.class, () ->
                PoStatusMachine.validateTransition(PoStatus.DELIVERED, PoStatus.DRAFT));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());

        assertThrows(ApiException.class, () ->
                PoStatusMachine.validateTransition(PoStatus.DELIVERED, PoStatus.CANCELLED));
    }

    @Test
    void testTerminalStateCancelledCannotTransition() {
        ApiException ex = assertThrows(ApiException.class, () ->
                PoStatusMachine.validateTransition(PoStatus.CANCELLED, PoStatus.SUBMITTED));
        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    void testInvalidSkipTransitions() {
        assertThrows(ApiException.class, () ->
                PoStatusMachine.validateTransition(PoStatus.DRAFT, PoStatus.CONFIRMED));
        assertThrows(ApiException.class, () ->
                PoStatusMachine.validateTransition(PoStatus.DRAFT, PoStatus.DELIVERED));
        assertThrows(ApiException.class, () ->
                PoStatusMachine.validateTransition(PoStatus.SUBMITTED, PoStatus.DELIVERED));
    }
}

