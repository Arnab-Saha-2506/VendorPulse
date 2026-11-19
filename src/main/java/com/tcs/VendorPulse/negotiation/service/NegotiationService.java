package com.tcs.VendorPulse.negotiation.service;

import com.tcs.VendorPulse.agreement.service.AgreementService;
import com.tcs.VendorPulse.audit.service.AuditService;
import com.tcs.VendorPulse.auth.security.CurrentUser;
import com.tcs.VendorPulse.common.enums.*;
import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import com.tcs.VendorPulse.common.event.DomainEvents.*;
import com.tcs.VendorPulse.common.paging.PagedResponse;
import com.tcs.VendorPulse.negotiation.domain.NegotiationRound;
import com.tcs.VendorPulse.negotiation.dto.NegotiationDtos.*;
import com.tcs.VendorPulse.negotiation.repository.NegotiationRoundRepository;
import com.tcs.VendorPulse.notification.service.NotificationService;
import com.tcs.VendorPulse.po.domain.PurchaseOrder;
import com.tcs.VendorPulse.po.repository.PurchaseOrderRepository;
import com.tcs.VendorPulse.settings.service.ScoringSettingsService;
import com.tcs.VendorPulse.user.domain.User;
import com.tcs.VendorPulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NegotiationService {

    private final NegotiationRoundRepository negotiationRoundRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final AgreementService agreementService;
    private final ScoringSettingsService scoringSettingsService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public NegotiationOverviewDto getNegotiationOverview(UUID poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current != null && current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        List<NegotiationRound> rounds = negotiationRoundRepository.findByPoIdOrderByRoundNoAsc(poId);
        Map<UUID, String> userNames = userRepository.findAllById(
                rounds.stream().map(NegotiationRound::getMadeBy).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        List<NegotiationRoundDto> roundDtos = rounds.stream().map(r -> NegotiationRoundDto.builder()
                .id(r.getId())
                .roundNo(r.getRoundNo())
                .fromParty(r.getFromParty())
                .action(r.getAction())
                .proposedDate(r.getProposedDate())
                .proposedPrice(r.getProposedPrice())
                .reason(r.getReason())
                .madeBy(r.getMadeBy())
                .madeByName(userNames.getOrDefault(r.getMadeBy(), "User"))
                .createdAt(r.getCreatedAt())
                .build()).collect(Collectors.toList());

        short currentRound = (short) rounds.size();
        short maxRounds = scoringSettingsService.getEntitySettings().getMaxNegotiationRounds();

        return NegotiationOverviewDto.builder()
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .currentRound(currentRound)
                .maxRounds(maxRounds)
                .awaitingParty(po.getAwaitingParty())
                .rounds(roundDtos)
                .build();
    }

    @Transactional
    public NegotiationOverviewDto respond(UUID poId, NegotiationResponseRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Purchase order not found"));

        CurrentUser current = CurrentUser.get();
        if (current == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        if (current.isSupplier() && !po.getSupplierId().equals(current.getSupplierId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        PartySide callerSide = current.getPartySide();

        // 1. Turn Rule Check (BR-09)
        if (po.getAwaitingParty() == null || po.getAwaitingParty() != callerSide) {
            throw new ApiException(ErrorCode.NOT_YOUR_TURN);
        }

        List<NegotiationRound> existingRounds = negotiationRoundRepository.findByPoIdOrderByRoundNoAsc(poId);
        short currentRoundNo = (short) existingRounds.size();
        short nextRoundNo = (short) (currentRoundNo + 1);
        short maxRounds = scoringSettingsService.getEntitySettings().getMaxNegotiationRounds();

        if (request.getAction() == NegotiationAction.COUNTER) {
            if (currentRoundNo >= maxRounds) {
                throw new ApiException(ErrorCode.NEGOTIATION_LIMIT_REACHED);
            }
            if (request.getProposedDate() == null || request.getProposedDate().isBefore(LocalDate.now())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Proposed date must be in the future");
            }
            if (request.getProposedPrice() == null || request.getProposedPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Proposed price must be positive");
            }

            PartySide nextAwaiting = (callerSide == PartySide.MANUFACTURER) ? PartySide.SUPPLIER : PartySide.MANUFACTURER;
            po.setAwaitingParty(nextAwaiting);
            purchaseOrderRepository.save(po);

            NegotiationRound counterRound = NegotiationRound.builder()
                    .id(UUID.randomUUID())
                    .poId(po.getId())
                    .roundNo(nextRoundNo)
                    .fromParty(callerSide)
                    .action(NegotiationAction.COUNTER)
                    .proposedDate(request.getProposedDate())
                    .proposedPrice(request.getProposedPrice())
                    .reason(request.getReason().trim())
                    .madeBy(current.getId())
                    .build();
            negotiationRoundRepository.save(counterRound);

            // Notify other party
            if (nextAwaiting == PartySide.SUPPLIER) {
                notificationService.notify(po.getSupplierId(), null, NotificationType.PROPOSAL,
                        "Counter proposal received for PO " + po.getPoNumber(), po.getId());
            } else {
                notificationService.notify(null, po.getCreatedBy(), NotificationType.PROPOSAL,
                        "Supplier countered proposal for PO " + po.getPoNumber(), po.getId());
            }

            auditService.log(current.getId(), current.getFullName(), "NEGOTIATION_COUNTER", "PO", po.getId().toString(), request.getReason());
            eventPublisher.publishEvent(PoCounteredEvent.builder()
                    .poId(po.getId())
                    .poNumber(po.getPoNumber())
                    .supplierId(po.getSupplierId())
                    .roundNo(nextRoundNo)
                    .fromParty(callerSide)
                    .proposedDate(request.getProposedDate())
                    .proposedPrice(request.getProposedPrice())
                    .reason(request.getReason())
                    .userId(current.getId())
                    .userName(current.getFullName())
                    .build());

        } else if (request.getAction() == NegotiationAction.REJECTED) {
            po.setStatus(PoStatus.CANCELLED);
            po.setCancelReason("Rejected during negotiation round " + nextRoundNo + ": " + request.getReason().trim());
            po.setCancelledBy(current.getId());
            po.setCancelledAt(Instant.now());
            po.setAwaitingParty(null);
            purchaseOrderRepository.save(po);

            NegotiationRound rejectRound = NegotiationRound.builder()
                    .id(UUID.randomUUID())
                    .poId(po.getId())
                    .roundNo(nextRoundNo)
                    .fromParty(callerSide)
                    .action(NegotiationAction.REJECTED)
                    .reason(request.getReason().trim())
                    .madeBy(current.getId())
                    .build();
            negotiationRoundRepository.save(rejectRound);

            notificationService.notify(po.getSupplierId(), null, NotificationType.CANCELLED,
                    "PO proposal " + po.getPoNumber() + " was rejected", po.getId());

            auditService.log(current.getId(), current.getFullName(), "NEGOTIATION_REJECTED", "PO", po.getId().toString(), request.getReason());
            eventPublisher.publishEvent(PoCancelledEvent.builder()
                    .poId(po.getId())
                    .poNumber(po.getPoNumber())
                    .supplierId(po.getSupplierId())
                    .cancelReason(request.getReason())
                    .userId(current.getId())
                    .userName(current.getFullName())
                    .build());

        } else if (request.getAction() == NegotiationAction.ACCEPTED) {
            // Find last proposing round
            NegotiationRound lastRound = existingRounds.get(existingRounds.size() - 1);
            LocalDate agreedDate = lastRound.getProposedDate() != null ? lastRound.getProposedDate() : po.getRequiredDate();
            BigDecimal agreedPrice = lastRound.getProposedPrice() != null ? lastRound.getProposedPrice() : po.getAgreedPriceTotal();

            NegotiationRound acceptRound = NegotiationRound.builder()
                    .id(UUID.randomUUID())
                    .poId(po.getId())
                    .roundNo(nextRoundNo)
                    .fromParty(callerSide)
                    .action(NegotiationAction.ACCEPTED)
                    .proposedDate(agreedDate)
                    .proposedPrice(agreedPrice)
                    .reason(request.getReason().trim())
                    .madeBy(current.getId())
                    .build();
            negotiationRoundRepository.save(acceptRound);

            UUID mfgUser;
            Instant mfgAt;
            UUID supUser;
            Instant supAt;

            if (callerSide == PartySide.MANUFACTURER) {
                mfgUser = current.getId();
                mfgAt = Instant.now();
                supUser = lastRound.getMadeBy();
                supAt = lastRound.getCreatedAt();
            } else {
                supUser = current.getId();
                supAt = Instant.now();
                mfgUser = lastRound.getMadeBy();
                mfgAt = lastRound.getCreatedAt();
            }

            agreementService.createAgreementOnAcceptance(po, agreedDate, agreedPrice, mfgUser, mfgAt, supUser, supAt);

            notificationService.notify(po.getSupplierId(), null, NotificationType.APPROVAL,
                    "PO " + po.getPoNumber() + " has been agreed and confirmed!", po.getId());
            notificationService.notify(null, po.getCreatedBy(), NotificationType.APPROVAL,
                    "PO " + po.getPoNumber() + " has been agreed and confirmed!", po.getId());

            auditService.log(current.getId(), current.getFullName(), "NEGOTIATION_ACCEPTED", "PO", po.getId().toString(), "Agreement created");
            eventPublisher.publishEvent(PoAgreedEvent.builder()
                    .poId(po.getId())
                    .poNumber(po.getPoNumber())
                    .supplierId(po.getSupplierId())
                    .agreedDeliveryDate(agreedDate)
                    .agreedPriceTotal(agreedPrice)
                    .acceptedByManufacturer(mfgUser)
                    .acceptedBySupplier(supUser)
                    .userId(current.getId())
                    .userName(current.getFullName())
                    .build());
        }

        return getNegotiationOverview(poId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProposalSummaryDto> getSupplierProposals(UUID supplierId, Pageable pageable) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierId(supplierId).stream()
                .filter(p -> p.getAwaitingParty() == PartySide.SUPPLIER && p.getStatus() == PoStatus.SUBMITTED)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), pos.size());
        List<PurchaseOrder> subList = start <= end ? pos.subList(start, end) : Collections.emptyList();

        List<ProposalSummaryDto> dtoList = subList.stream().map(po -> {
            Optional<NegotiationRound> lastRound = negotiationRoundRepository.findFirstByPoIdOrderByRoundNoDesc(po.getId());
            return ProposalSummaryDto.builder()
                    .poId(po.getId())
                    .poNumber(po.getPoNumber())
                    .round(lastRound.map(NegotiationRound::getRoundNo).orElse((short) 1))
                    .dueDate(lastRound.map(NegotiationRound::getProposedDate).orElse(po.getRequiredDate()))
                    .proposedDate(lastRound.map(NegotiationRound::getProposedDate).orElse(po.getRequiredDate()))
                    .proposedPrice(lastRound.map(NegotiationRound::getProposedPrice).orElse(po.getAgreedPriceTotal()))
                    .reason(lastRound.map(NegotiationRound::getReason).orElse("PO Proposal"))
                    .build();
        }).collect(Collectors.toList());

        Page<ProposalSummaryDto> page = new PageImpl<>(dtoList, pageable, pos.size());
        return PagedResponse.from(page);
    }
}

