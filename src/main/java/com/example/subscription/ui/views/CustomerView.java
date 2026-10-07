package com.example.subscription.ui.views;

import com.example.subscription.domain.catalogs.Customer;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

/**
 * UI shape of the {@link Customer} catalog: list columns and form layout.
 *
 * <p>The inherited {@code description} field carries the customer's name — in the list and
 * on the form it is relabelled to "Name", which is what a user actually recognises. The
 * registration date is formatted as a date-only value so it reads as a business fact rather
 * than a raw timestamp.</p>
 */
@Component
public class CustomerView implements EntityView<Customer> {

    @Override
    public Class<Customer> entity() {
        return Customer.class;
    }

    @Override
    public void list(ListSpec<Customer> list) {
        // Code first (the framework generates "C-…"), then the human name, then the
        // attributes a manager scans when picking a customer — status, contacts, signup date.
        list.columns(Customer::getCode, Customer::getDescription, Customer::getStatus,
                        Customer::getEmail, Customer::getPhone, Customer::getRegistrationDate)
                .label(Customer::getDescription, "Name")
                .sortBy(Customer::getDescription, false);
    }

    @Override
    public void fields(EntityConfigBuilder<Customer> f) {
        f.field(Customer::getDescription).order(0).label("Name")
                .field(Customer::getStatus).order(1)
                .field(Customer::getEmail).order(2)
                .field(Customer::getPhone).order(3)
                .field(Customer::getRegistrationDate).order(4).format("dd-MM-yyyy");
    }
}