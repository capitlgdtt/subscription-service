package com.example.subscription.ui.views;

import com.example.subscription.domain.documents.Payment;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

/**
 * UI shape of the {@link Payment} document.
 *
 * <p>The list is ordered newest first — payments are an append-only ledger of money in, so
 * the top of the list is always the most recent activity. The "Posted" flag is shown as its
 * own column: an unposted payment has not yet touched the customer's balance, and a manager
 * looking at a customer's history needs to know the difference at a glance.</p>
 *
 * <p>The {@code method} filter is declared so that charts on the dashboard can group payments
 * by method — onno derives a {@code methodDisplay} label from the filter declaration, and the
 * dashboard's "Payments by method" pie relies on it.</p>
 */
@Component
public class PaymentView implements EntityView<Payment> {

    @Override
    public Class<Payment> entity() {
        return Payment.class;
    }

    @Override
    public void list(ListSpec<Payment> list) {
        list.columns(Payment::getNumber, Payment::getDate, Payment::getCustomer,
                        Payment::getAmount, Payment::getMethod, Payment::isPosted)
                .label(Payment::getAmount, "Amount")
                .label(Payment::getMethod, "Method")
                .label(Payment::isPosted, "Posted")
                .sortBy(Payment::getDate, true);

        // Declaring the filter does two things: it gives the list a filter bar, and it
        // registers the enum with the metadata so charts can `groupBy: "method"` and get
        // a human-readable label instead of the raw constant.
        list.filter(Payment::getMethod).label("Method").multiOptions();
    }

    @Override
    public void fields(EntityConfigBuilder<Payment> f) {
        // Short paired fields sit side by side (width "half"): customer+method, then
        // date+amount. This is the "read like a receipt" layout a manager is used to.
        f.field(Payment::getCustomer).order(0).width("half")
                .field(Payment::getMethod).order(1).width("half")
                .field(Payment::getDate).order(2).width("half").format("dd-MM-yyyy HH:mm")
                .field(Payment::getAmount).order(3).width("half").format("currency:USD")
                .field(Payment::getNote).order(4).widget("textarea");
    }
}