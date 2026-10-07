package com.example.subscription.domain.catalogs;

import com.example.subscription.domain.enumerations.CustomerStatus;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Catalog;
import su.onno.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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