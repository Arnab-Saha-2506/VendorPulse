package com.tcs.VendorPulse.contact.api;

import com.tcs.VendorPulse.contact.service.ContactService;
import com.tcs.VendorPulse.notification.dto.NotificationDtos.ContactMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Public contact us form APIs")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Submit contact message", description = "Submit contact form from the public website")
    public ResponseEntity<Void> submitContactMessage(@Valid @RequestBody ContactMessageRequest request) {
        contactService.submitContactMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

