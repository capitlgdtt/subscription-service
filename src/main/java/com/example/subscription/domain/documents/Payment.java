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

/**
 * A top-up of a customer's {@link AccountBalance} — the only way money enters the system.
 *
 * <p>Posting writes a single receipt movement into the balance register, identified by the
 * customer. The amount is not validated against any external ledger: the service trusts the
 * declared {@code amount}, and the payment method is recorded for reporting only. Whether a
 * subscription can then be paid for out of that balance is the subscription's problem, not
 * the payment's.</p>
 *
 * <p>A payment is intentionally <b>not</b> reversible by editing after posting — the balance
 * movement is fixed once written. To correct a wrong amount, post a compensating payment.</p>
 */
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
        // Idempotent: onFilling runs on every save of a new entity, so an explicitly set
        // date — e.g. by a seeder or an importer — must not be overwritten.
        if (getDate() == null) {
            setDate(LocalDateTime.now());
        }
    }

    @Override
    public List<BusinessRule> rules() {
        return List.of(
                new BusinessRule("customer-required", "Choose a customer",
                        () -> customer != null),
                // A non-positive amount would be a silent no-op at posting; rejecting it here
                // surfaces the mistake to the user instead.
                new BusinessRule("amount-positive", "Amount must be greater than zero",
                        () -> amount != null && amount.signum() > 0));
    }

    @Override
    public void handlePosting(PostingContext context) {
        // Defensive guard: the rules above already reject bad input, but a payment can also
        // arrive via import or a direct API call that bypasses the form. Posting without a
        // customer or a positive amount would write a meaningless register row.
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