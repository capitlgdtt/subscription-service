package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.AccountBalanceRepository;
import com.example.subscription.repositories.CustomerRepository;
import com.example.subscription.repositories.RevenueRepository;
import com.example.subscription.repositories.SubscriptionRepository;
import com.example.subscription.repositories.TariffRepository;
import su.onno.posting.PostingService;
import su.onno.types.Ref;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the "can't post an underfunded subscription" guarantee. It is not implemented by a
 * hand-written check in {@link Subscription}: it falls out of declaring {@link
 * com.example.subscription.domain.registers.AccountBalance} as a BALANCE register with
 * {@code allowNegative = false}. The posting engine refuses to write a movement that would
 * drive the balance below zero, and nothing is persisted.
 */
class SubscriptionInsufficientFundsTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired AccountBalanceRepository balances;
    @Autowired RevenueRepository revenues;
    @Autowired PostingService posting;

    @Test
    void posting_withInsufficientBalance_isRejected_andWritesNothing() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Customer customer = newCustomer("Acme-" + tag);
        Tariff monthly = newTariff("Monthly-" + tag, "100.00", 30);

        // Account has no payments — balance is zero.
        Subscription subscription = newSubscription(customer, monthly, 1);
        Subscription saved = subscriptions.save(subscription);

        assertThatThrownBy(() -> posting.post(saved))
                .hasMessageContaining("Insufficient amount")
                .hasMessageContaining("Account Balances");

        // Balance stays zero — no partial receipt written.
        assertThat(balanceOf(customer)).isEqualByComparingTo("0.00");

        // No revenue was recognised either.
        assertThat(revenueFor(customer)).isEmpty();
    }

    // --- helpers -------------------------------------------------------------------------------

    private Customer newCustomer(String name) {
        Customer c = new Customer();
        c.setDescription(name);
        return customers.save(c);
    }

    private Tariff newTariff(String name, String price, int periodDays) {
        Tariff t = new Tariff();
        t.setDescription(name);
        t.setPrice(new BigDecimal(price));
        t.setPeriodDays(periodDays);
        t.setAvailable(true);
        return tariffs.save(t);
    }

    private Subscription newSubscription(Customer customer, Tariff tariff, int periods) {
        Subscription sub = new Subscription();
        sub.setCustomer(Ref.of(Customer.class, customer.getId()));
        sub.setStatus(SubscriptionStatus.DRAFT);
        sub.setStartDate(LocalDate.now());
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(Ref.of(Tariff.class, tariff.getId()));
        line.setPeriods(periods);
        sub.getLines().add(line);
        return sub;
    }

    private BigDecimal balanceOf(Customer customer) {
        return balances.getBalance().stream()
                .filter(r -> r.getCustomer() != null
                        && r.getCustomer().id().equals(customer.getId()))
                .map(b -> b.getAmount() == null ? BigDecimal.ZERO : b.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private java.util.List<com.example.subscription.domain.registers.Revenue> revenueFor(Customer customer) {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        return revenues.getTurnover(from, to).stream()
                .filter(r -> r.getCustomer() != null
                        && r.getCustomer().id().equals(customer.getId()))
                .toList();
    }
}