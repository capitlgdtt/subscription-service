package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Payment;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.PaymentMethod;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.domain.registers.AccountBalance;
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
 * Verifies the posting contract of {@link Subscription}: a posted subscription draws its total
 * from the customer's {@link AccountBalance} and recognises revenue in the {@link Revenue}
 * turnover register. A subscription is posted through {@link PostingService#post} — that pipeline
 * calls {@code beforeWrite} itself, so the test does not need to pre-compute prices.
 *
 * <p>Traces to Level 2 requirements on posting: the subscription amount is drawn from the
 * customer's account at posting time, and revenue is recognised by tariff and customer.</p>
 */
@Tag("level-2")
class SubscriptionPostingTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired PaymentRepository payments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired AccountBalanceRepository balances;
    @Autowired RevenueRepository revenues;
    @Autowired PostingService posting;

    @Test
    @DisplayName("[Level 2 · posting] Posting a subscription draws the balance and recognises revenue")
    void posting_drawsBalance_andRecognisesRevenue() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Ref<Customer> customer = newCustomer("Acme-" + tag);
        Ref<Tariff> monthly = newTariff("Monthly-" + tag, "30.00", 30);

        // Fund the account with 100 via a posted Payment.
        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setMethod(PaymentMethod.CARD);
        payments.save(payment);
        posting.post(payment);

        assertThat(balanceOf(customer)).isEqualByComparingTo("100.00");

        // 2 periods × 30.00 = 60.00 — affordable.
        Subscription subscription = newSubscription(customer, monthly, 2);
        Subscription saved = subscriptions.save(subscription);
        posting.post(saved);

        // Balance decreased by the line amount, not the tariff's period length.
        assertThat(balanceOf(customer)).isEqualByComparingTo("40.00");

        // Revenue recognises the amount and the periods sold.
        List<Revenue> revenue = revenueFor(customer);
        assertThat(revenue).hasSize(1);
        Revenue entry = revenue.getFirst();
        assertThat(entry.getAmount()).isEqualByComparingTo("60.00");
        assertThat(entry.getPeriods()).isEqualByComparingTo("2");
        assertThat(entry.getTariff().id()).isEqualTo(monthly.id());
        // The denormalised name is snapshotted at posting time so charts can group by label.
        assertThat(entry.getTariffName()).isNotBlank();
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
        sub.setStatus(SubscriptionStatus.DRAFT);
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
                .map(AccountBalance::getAmount)
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