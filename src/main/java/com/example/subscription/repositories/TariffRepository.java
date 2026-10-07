package com.example.subscription.repositories;

import com.example.subscription.domain.catalogs.Tariff;
import su.onno.repository.CatalogRepository;

/**
 * Typed repository for {@link Tariff}.
 *
 * <p>Read in two hot paths: {@code Subscription.beforeWrite()} to refresh each line's price
 * and period length from the current tariff, and {@code Subscription.handlePosting()} to
 * snapshot the tariff's name onto the revenue register row. Both look the tariff up by id,
 * so the parent's access rules (which allow MANAGER/ADMIN) apply as usual.</p>
 */
public interface TariffRepository extends CatalogRepository<Tariff> {
}