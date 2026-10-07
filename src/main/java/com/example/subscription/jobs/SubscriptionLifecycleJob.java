package com.example.subscription.jobs;

import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.SubscriptionRepository;
import su.onno.annotations.ScheduledJob;
import su.onno.jobs.BackgroundTask;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Advances subscriptions through their date-driven lifecycle.
 *
 * <p>Two transitions are handled:</p>
 * <ul>
 *   <li>{@code DRAFT → ACTIVE} — the start date has arrived and the document has been posted.
 *       An unposted draft stays a draft on purpose: nothing has been drawn from the customer's
 *       balance, so it must not appear as a running subscription even if its start date is in
 *       the past.</li>
 *   <li>{@code ACTIVE → EXPIRED} — the end date has passed.</li>
 * </ul>
 *
 * <p>{@link SubscriptionStatus#CANCELLED} is terminal and is never touched — a cancelled
 * subscription keeps its status and its audit reason forever.</p>
 *
 * <p>onno registers this bean at startup via {@code ScheduledJobRegistrar}: every Spring bean
 * that both implements {@link BackgroundTask} and carries {@link ScheduledJob} is scheduled
 * against JobRunr's cron. The cron below fires every minute, which makes the demo visibly
 * responsive; a production deployment would use an hourly or daily cadence
 * (e.g. {@code 0 0 * * * *}).</p>
 *
 * <p>The job is idempotent: each pass re-reads every active document and writes only the rows
 * whose status actually needs to change, so a missed or duplicated trigger causes no harm.</p>
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
        // findAllActive() honours the soft-delete flag — a subscription marked for deletion
        // must not be resurrected by a status change.
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