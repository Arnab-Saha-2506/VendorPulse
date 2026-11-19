package com.tcs.VendorPulse.message.service;

import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.enums.NotificationType;
import com.tcs.VendorPulse.common.enums.PartySide;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.message.domain.PoMessage;
import com.tcs.VendorPulse.message.dto.MessageDtos.*;
import com.tcs.VendorPulse.message.repository.PoMessageRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PoMessageService {

    private final PoMessageRepository messageRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public MessagePollResponse getMessages(UUID poId, UUID afterMessageId, int limit) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        validateAccess(po, current);

        Instant afterTime = null;
        if (afterMessageId != null) {
            afterTime = messageRepository.findById(afterMessageId).map(PoMessage::getCreatedAt).orElse(null);
        }

        List<PoMessage> rawList = messageRepository.findMessagesAfter(poId, afterTime);
        if (rawList.size() > limit) {
            rawList = rawList.subList(0, limit);
        }

        PartySide callerSide = current.getPartySide();
        long unreadCount = messageRepository.countByPoIdAndSenderSideNotAndReadAtIsNull(poId, callerSide);

        Map<UUID, String> userNames = userRepository.findAllById(
                rawList.stream().map(PoMessage::getSenderId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        List<PoMessageDto> dtos = rawList.stream().map(m -> PoMessageDto.builder()
                .id(m.getId())
                .poId(m.getPoId())
                .senderId(m.getSenderId())
                .senderName(userNames.getOrDefault(m.getSenderId(), "User"))
                .senderSide(m.getSenderSide())
                .body(m.getBody())
                .readAt(m.getReadAt())
                .createdAt(m.getCreatedAt())
                .build()).collect(Collectors.toList());

        return MessagePollResponse.builder()
                .messages(dtos)
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional
    public PoMessageDto sendMessage(UUID poId, SendMessageRequest request) {
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Message body cannot be empty");
        }

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        validateAccess(po, current);

        PartySide senderSide = current.getPartySide();

        PoMessage message = PoMessage.builder()
                .id(UUID.randomUUID())
                .poId(po.getId())
                .senderId(current.getId())
                .senderSide(senderSide)
                .body(request.getBody().trim())
                .build();

        message = messageRepository.save(message);

        // Notify other side
        if (senderSide == PartySide.MANUFACTURER) {
            notificationService.notify(po.getSupplierId(), null, NotificationType.MESSAGE,
                    "New message on PO " + po.getPoNumber() + " from " + current.getFullName(), po.getId());
        } else {
            notificationService.notify(null, po.getCreatedBy(), NotificationType.MESSAGE,
                    "New message on PO " + po.getPoNumber() + " from supplier", po.getId());
        }

        return PoMessageDto.builder()
                .id(message.getId())
                .poId(message.getPoId())
                .senderId(message.getSenderId())
                .senderName(current.getFullName())
                .senderSide(message.getSenderSide())
                .body(message.getBody())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    @Transactional
    public void markThreadRead(UUID poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        validateAccess(po, current);

        messageRepository.markOtherSideMessagesAsRead(poId, current.getPartySide(), Instant.now());
    }

    private void validateAccess(PurchaseOrder po, CurrentUser current) {
        if (current == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        if (current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
}

