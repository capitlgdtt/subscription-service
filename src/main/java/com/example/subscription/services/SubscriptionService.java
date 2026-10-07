package com.example.subscription.services;

import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.SubscriptionRepository;

import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Business operations on {@link Subscription} documents that are not tied to the write
 * pipeline or the UI.
 *
 * <p>Kept deliberately small: only the cancellation flow lives here, because it is called
 * from two different places — the row action and the detail-header action on
 * {@link com.example.subscription.ui.views.SubscriptionView} — and both deserve the same
 * behaviour. Extracting it into a bean also makes the flow testable without reaching into
 * the view layer.</p>
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;

    public SubscriptionService(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    /**
     * Cancels a subscription: sets its status to {@link SubscriptionStatus#CANCELLED} and
     * stores the reason for the audit trail.
     *
     * <p>Does not reverse any register movements a previous posting may have written. The
     * intended flow is to cancel before posting; cancelling a posted document leaves its
     * movements in place, which is the correct behaviour for an accounting ledger.</p>
     *
     * @return the updated document, or {@code null} if no subscription with this id exists
     */
    public Subscription cancel(UUID id, String reason) {
        Objects.requireNonNull(id, "id");
        return subscriptions.findById(id).map(s -> {
            s.setStatus(SubscriptionStatus.CANCELLED);
            s.setCancelReason(reason);
            return subscriptions.save(s);
        }).orElse(null);
    }
}