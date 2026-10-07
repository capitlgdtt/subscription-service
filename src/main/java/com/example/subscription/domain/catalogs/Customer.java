package com.example.subscription.domain.catalogs;

import com.example.subscription.domain.enumerations.CustomerStatus;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A subscriber — the party that owns an {@link com.example.subscription.domain.registers.AccountBalance
 * account balance}, tops it up with {@link com.example.subscription.domain.documents.Payment payments},
 * and buys {@link com.example.subscription.domain.documents.Subscription subscriptions}.
 *
 * <p>Master data: staff add and edit customers at runtime without a code change, which is why
 * this is a {@code @Catalog} and not an enumeration. The human-readable name comes from the
 * inherited {@code description} field; the framework generates a {@code C-} code for each
 * record.</p>
 *
 * <p>Both roles are allowed to read and write — the service has no fine-grained customer-level
 * permissions; the {@code @AccessControl} annotation is kept explicit so it can be tightened
 * without restructuring the class.</p>
 */
@Catalog(name = "Customers", title = "Customer", codePrefix = "C-", context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"}, writeRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class Customer extends CatalogObject {

    @Attribute(displayName = "Client status")
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Attribute(displayName = "Email", length = 200, email = true)
    private String email;

    @Attribute(displayName = "Phone", length = 50)
    private String phone;

    @Attribute(displayName = "Registration date")
    private LocalDate registrationDate = LocalDate.now();
}