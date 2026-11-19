package com.tcs.VendorPulse.po;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.VendorPulse.auth.dto.LoginRequest;
import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.enums.PoStatus;
import com.tcs.VendorPulse.common.enums.UserRole;
import com.tcs.VendorPulse.material.domain.Material;
import com.tcs.VendorPulse.material.repository.MaterialRepository;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.dto.PoDtos.CreatePoRequest;
import com.tcs.VendorPulse.po.dto.PoDtos.PoLineRequest;
import com.tcs.VendorPulse.supplier.domain.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PurchaseOrderIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private MaterialRepository materialRepository;

    private String pmToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        pmToken = loginAndGetToken("admin.pm@vendorpulse.com", "Password123!", UserRole.PROCUREMENT_MANAGER);
        adminToken = loginAndGetToken("admin@vendorpulse.com", "Password123!", UserRole.SYSTEM_ADMIN);
    }

    private String loginAndGetToken(String email, String password, UserRole role) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setRole(role);

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(res.getResponse().getContentAsString(), Map.class);
        return (String) body.get("accessToken");
    }

    @Test
    void testCreateSubmitAndGetPurchaseOrder() throws Exception {
        Supplier supplier = supplierRepository.findAll().stream().findFirst().orElseThrow();
        Material material = materialRepository.findAll().stream().findFirst().orElseThrow();

        // 1. Create Draft PO
        CreatePoRequest createRequest = CreatePoRequest.builder()
                .supplierId(supplier.getId())
                .requiredDate(LocalDate.now().plusDays(20))
                .cutoffDays((short) 7)
                .notes("Automated test draft PO")
                .lines(List.of(
                        PoLineRequest.builder()
                                .materialId(material.getId())
                                .quantity(new BigDecimal("150.00"))
                                .unitPrice(new BigDecimal("25.50"))
                                .requiredDate(LocalDate.now().plusDays(20))
                                .build()
                ))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/purchase-orders")
                        .header("Authorization", "Bearer " + pmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        PurchaseOrder createdPo = objectMapper.readValue(createRes.getResponse().getContentAsString(), PurchaseOrder.class);
        UUID poId = createdPo.getId();

        // 2. Submit Draft PO
        mockMvc.perform(post("/api/v1/purchase-orders/" + poId + "/submit")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PoStatus.SUBMITTED.name()))
                .andExpect(jsonPath("$.awaitingParty").value(PartySide.SUPPLIER.name()));

        // 3. Get PO Details
        mockMvc.perform(get("/api/v1/purchase-orders/" + poId)
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(poId.toString()))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines[0].materialId").value(material.getId().toString()));

        // 4. Search POs
        mockMvc.perform(get("/api/v1/purchase-orders")
                        .header("Authorization", "Bearer " + pmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testAdminJobsRunAll() throws Exception {
        mockMvc.perform(post("/api/v1/admin/jobs/run-all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobsRun").value(5))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
