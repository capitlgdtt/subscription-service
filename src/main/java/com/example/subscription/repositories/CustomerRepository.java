package com.example.subscription.repositories;

import com.example.subscription.domain.catalogs.Customer;
import su.onno.repository.CatalogRepository;

/**
 * Typed repository for {@link Customer}, offering the catalog flavour of CRUD
 * (soft-delete-aware finders, code lookup) rather than the document or register flavour.
 */
public interface CustomerRepository extends CatalogRepository<Customer> {
}