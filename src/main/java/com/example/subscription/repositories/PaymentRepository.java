package com.example.subscription.repositories;

import com.example.subscription.domain.documents.Payment;
import su.onno.repository.DocumentRepository;

/**
 * Typed repository for {@link Payment}. Documents are soft-deleted, so business code
 * should prefer the {@code findActiveBy*} finders that {@link DocumentRepository} exposes
 * over the inherited {@code findAll}/{@code findById}.
 */
public interface PaymentRepository extends DocumentRepository<Payment> {
}