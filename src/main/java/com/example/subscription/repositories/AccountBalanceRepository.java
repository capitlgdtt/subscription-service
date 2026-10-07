package com.example.subscription.repositories;

import com.example.subscription.domain.registers.AccountBalance;
import su.onno.repository.RegisterRepository;

/**
 * Read access to the {@link AccountBalance} register — balances and turnover.
 *
 * <p>Extends {@link RegisterRepository}, not {@code CatalogRepository}/{@code DocumentRepository}:
 * registers expose their own query surface ({@code getBalance}, {@code getTurnover},
 * {@code getRecordsByDocument}) instead of CRUD. The framework writes register rows through
 * the posting pipeline, not through this repository — tests and reports use it for reading.</p>
 */
public interface AccountBalanceRepository extends RegisterRepository<AccountBalance> {
}