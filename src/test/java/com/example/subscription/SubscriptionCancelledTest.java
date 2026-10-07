package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Payment;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.PaymentMethod;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.domain.registers.Revenue;
import com.example.subscription.repositories.AccountBalanceRepository;
import com.example.subscription.repositories.CustomerRepository;
import com.example.subscription.repositories.PaymentRepository;
import com.example.subscription.repositories.RevenueRepository;
import com.example.subscription.repositories.SubscriptionRepository;
import com.example.subscription.repositories.TariffRepository;
import su.onno.posting.PostingService;
import su.onno.types.Ref;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a subscription in {@link SubscriptionStatus#CANCELLED} is a no-op at posting:
 * no balance movement is written and no revenue is recognised. The guard lives in
 * {@code Subscription.handlePosting}: it returns early when the status is cancelled.
 *
 * <p>Traces to the Level 2 requirement that a cancelled subscription creates no movements.</p>
 */
@Tag("level-2")
class SubscriptionCancelledTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired PaymentRepository payments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired AccountBalanceRepository balances;
    @Autowired RevenueRepository revenues;
    @Autowired PostingService posting;

    @Test
    @DisplayName("[Level 2 · posting] Cancelled subscription writes no balance and no revenue movements")
    void posting_cancelledSubscription_doesNotTouchBalancesOrRevenue() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Ref<Customer> customer = newCustomer("Acme-" + tag);
        Ref<Tariff> monthly = newTariff("Monthly-" + tag, "30.00", 30);

        // Fund the account with 100, so a wrong post would be affordable and thus visible.
        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setMethod(PaymentMethod.CARD);
        payments.save(payment);
        posting.post(payment);

        assertThat(balanceOf(customer)).isEqualByComparingTo("100.00");

        // Cancelled subscription with a reason — the audit trail is the reason field.
        Subscription subscription = newSubscription(customer, monthly, 1);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelReason("Customer changed their mind");
        Subscription saved = subscriptions.save(subscription);

        posting.post(saved);

        // Neither balance nor revenue changed.
        assertThat(balanceOf(customer)).isEqualByComparingTo("100.00");
        assertThat(revenueFor(customer)).isEmpty();
    }

    // --- helpers -------------------------------------------------------------------------------

    @SuppressWarnings("SameParameterValue")
    private Ref<Customer> newCustomer(String name) {
        Customer c = new Customer();
        c.setDescription(name);
        return Ref.of(Customer.class, Objects.requireNonNull(customers.save(c).getId()));
    }

    @SuppressWarnings("SameParameterValue")
    private Ref<Tariff> newTariff(String name, String price, int periodDays) {
        Tariff t = new Tariff();
        t.setDescription(name);
        t.setPrice(new BigDecimal(price));
        t.setPeriodDays(periodDays);
        t.setAvailable(true);
        return Ref.of(Tariff.class, Objects.requireNonNull(tariffs.save(t).getId()));
    }

    @SuppressWarnings("SameParameterValue")
    private Subscription newSubscription(Ref<Customer> customer, Ref<Tariff> tariff, int periods) {
        Subscription sub = new Subscription();
        sub.setCustomer(customer);
        sub.setStartDate(LocalDate.now());
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(tariff);
        line.setPeriods(periods);
        sub.getLines().add(line);
        return sub;
    }

    private BigDecimal balanceOf(Ref<Customer> customer) {
        return balances.getBalance().stream()
                .filter(r -> r.getCustomer() != null
                        && r.getCustomer().id().equals(customer.id()))
                .map(b -> b.getAmount() == null ? BigDecimal.ZERO : b.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Revenue> revenueFor(Ref<Customer> customer) {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        return revenues.getTurnover(from, to).stream()
                .filter(r -> r.getCustomer() != null
                        && r.getCustomer().id().equals(customer.id()))
                .toList();
    }
}