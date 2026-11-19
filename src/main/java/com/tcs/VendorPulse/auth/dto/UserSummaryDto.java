package com.tcs.VendorPulse.auth.dto;

import com.tcs.VendorPulse.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private UUID supplierId;
}

