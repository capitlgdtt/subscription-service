package com.example.subscription.repositories;

import com.example.subscription.domain.documents.Subscription;
import su.onno.repository.DocumentRepository;

/**
 * Typed repository for {@link Subscription}.
 *
 * <p>Used by {@link com.example.subscription.jobs.SubscriptionLifecycleJob} to scan active
 * subscriptions and by the cancellation action on the entity view to load and update a single
 * document. As with every document repository, prefer the soft-delete-aware finders.</p>
 */
public interface SubscriptionRepository extends DocumentRepository<Subscription> {
}