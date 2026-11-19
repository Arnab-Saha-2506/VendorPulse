package com.tcs.VendorPulse.contact.service;

import com.tcs.VendorPulse.contact.domain.ContactMessage;
import com.tcs.VendorPulse.contact.repository.ContactMessageRepository;
import com.tcs.VendorPulse.notification.dto.NotificationDtos.ContactMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    @Transactional
    public void submitContactMessage(ContactMessageRequest request) {
        ContactMessage message = ContactMessage.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .subject(request.getSubject().trim())
                .message(request.getMessage().trim())
                .handled(false)
                .build();
        contactMessageRepository.save(message);
    }
}

