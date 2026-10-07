package com.example.subscription.domain.documents;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.enumerations.PaymentMethod;
import com.example.subscription.domain.registers.AccountBalance;
import su.onno.annotations.AccessControl;
import su.onno.annotations.Attribute;
import su.onno.annotations.Document;
import su.onno.lifecycle.OnFillingHandler;
import su.onno.lifecycle.Postable;
import su.onno.model.DocumentObject;
import su.onno.posting.PostingContext;
import su.onno.rules.BusinessRule;
import su.onno.rules.Validated;
import su.onno.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(name = "Payments", title = "Payment", numberPrefix = "P-", context = "Sales")
@AccessControl(readRoles = {"MANAGER", "ADMIN"}, writeRoles = {"MANAGER", "ADMIN"})
@Getter
@Setter
public class Payment extends DocumentObject implements OnFillingHandler, Validated, Postable {

    @Attribute(displayName = "Customer", required = true)
    private Ref<Customer> customer;

    @Attribute(displayName = "Amount", precision = 15, scale = 2, required = true)
    private BigDecimal amount = BigDecimal.ZERO;

    @Attribute(displayName = "Payment method")
    private PaymentMethod method = PaymentMethod.CARD;

    @Attribute(displayName = "Note", length = 500)
    private String note;

    @Override
    public void onFilling() {
        if (getDate() == null) {
            setDate(LocalDateTime.now());
        }
    }

    @Override
    public List<BusinessRule> rules() {
        return List.of(
                new BusinessRule("customer-required", "Choose a customer",
                        () -> customer != null),
                new BusinessRule("amount-positive", "Amount must be greater than zero",
                        () -> amount != null && amount.signum() > 0));
    }

    @Override
    public void handlePosting(PostingContext context) {
        if (customer == null || amount == null || amount.signum() <= 0) {
            return;
        }
        var balance = context.movements(AccountBalance.class);
        balance.addReceipt(r -> {
            r.setCustomer(customer);
            r.setAmount(amount);
        });
    }
}