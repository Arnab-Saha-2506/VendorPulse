package com.tcs.VendorPulse.auth.controller;

import com.tcs.VendorPulse.auth.dto.LoginRequestDTO;
import com.tcs.VendorPulse.common.enums.Role;
import com.tcs.VendorPulse.security.CustomUserDetails;
import com.tcs.VendorPulse.security.SecurityUtils;
import com.tcs.VendorPulse.supplier.entity.Supplier;
import com.tcs.VendorPulse.supplier.repository.SupplierRepository;
import com.tcs.VendorPulse.user.entity.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;

    public AuthController(AuthenticationManager am,
                          SecurityContextRepository scr,
                          UserRepository ur,
                          SupplierRepository sr) {
        this.authManager = am;
        this.securityContextRepository = scr;
        this.userRepository = ur;
        this.supplierRepository = sr;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        User user = SecurityUtils.getCurrentUser(auth, userRepository);
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("email", user.getEmail());
        resp.put("role", user.getRole().name());

        if (user.getRole() == Role.SUPPLIER) {
            supplierRepository.findByUserId(user.getId())
                    .ifPresent(s -> resp.put("supplierId", s.getId()));
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        securityContextRepository.saveContext(emptyContext, request, response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = SecurityUtils.getCurrentUser(auth, userRepository);
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("email", user.getEmail());
        resp.put("role", user.getRole().name());

        if (user.getRole() == Role.SUPPLIER) {
            supplierRepository.findByUserId(user.getId())
                    .ifPresent(s -> resp.put("supplierId", s.getId()));
        }

        return ResponseEntity.ok(resp);
    }
}
