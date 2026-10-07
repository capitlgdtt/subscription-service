package com.example.subscription.repositories;

import com.example.subscription.domain.registers.Revenue;
import su.onno.repository.RegisterRepository;

/**
 * Read access to the {@link Revenue} register — turnover sliced by tariff and customer.
 * Used by tests to assert what a posting actually wrote; the dashboard reads the register
 * through the framework's widget layer, not through this repository.
 */
public interface RevenueRepository extends RegisterRepository<Revenue> {
}