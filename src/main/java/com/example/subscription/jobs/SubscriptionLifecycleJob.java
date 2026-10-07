package com.example.subscription.jobs;

import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.SubscriptionRepository;
import su.onno.annotations.ScheduledJob;
import su.onno.jobs.BackgroundTask;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Background task that advances subscriptions through their date-driven lifecycle.
 *
 * <p>Two transitions are handled:</p>
 * <ul>
 *   <li>{@code DRAFT → ACTIVE} — the start date has arrived and the document has been posted.
 *       An unposted draft stays a draft: nothing has been drawn from the customer's balance,
 *       so it must not appear as a running subscription.</li>
 *   <li>{@code ACTIVE → EXPIRED} — the end date has passed.</li>
 * </ul>
 *
 * <p>A {@link SubscriptionStatus#CANCELLED} subscription is never touched: it is a terminal
 * state and carries an audit reason.</p>
 *
 * <p>The task is registered by the framework on startup (see {@code ScheduledJobRegistrar});
 * the cron below fires every minute, which is convenient for a demo. In production use an
 * hourly or daily cadence, e.g. {@code 0 0 * * * *}.</p>
 */
@Component
@ScheduledJob(name = "subscription-lifecycle", cron = "0 * * * * *")
public class SubscriptionLifecycleJob implements BackgroundTask {

    private final SubscriptionRepository subscriptions;

    public SubscriptionLifecycleJob(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public void execute() {
        LocalDate today = LocalDate.now();
        for (Subscription s : subscriptions.findAllActive()) {
            if (s.getStatus() == SubscriptionStatus.CANCELLED) {
                continue;
            }

            if (s.getStatus() == SubscriptionStatus.DRAFT
                    && s.isPosted()
                    && s.getStartDate() != null
                    && !s.getStartDate().isAfter(today)) {
                s.setStatus(SubscriptionStatus.ACTIVE);
                subscriptions.save(s);
                continue;
            }

            if (s.getStatus() == SubscriptionStatus.ACTIVE
                    && s.getEndDate() != null
                    && s.getEndDate().isBefore(today)) {
                s.setStatus(SubscriptionStatus.EXPIRED);
                subscriptions.save(s);
            }
        }
    }
}