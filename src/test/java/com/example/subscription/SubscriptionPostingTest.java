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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the posting contract of {@link Subscription}: a posted subscription draws its total
 * from the customer's {@link AccountBalance} and recognises revenue in the {@link Revenue}
 * turnover register. A subscription is posted through {@link PostingService#post} — that pipeline
 * calls {@code beforeWrite} itself, so the test does not need to pre-compute prices.
 */
class SubscriptionPostingTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired PaymentRepository payments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired AccountBalanceRepository balances;
    @Autowired RevenueRepository revenues;
    @Autowired PostingService posting;

    @Test
    void posting_drawsBalance_andRecognisesRevenue() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Customer customer = newCustomer("Acme-" + tag);
        Tariff monthly = newTariff("Monthly-" + tag, "30.00", 30);

        // Fund the account with 100 via a posted Payment.
        Payment payment = new Payment();
        payment.setCustomer(Ref.of(Customer.class, customer.getId()));
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
        var revenue = revenueFor(customer);
        assertThat(revenue).hasSize(1);
        assertThat(revenue.get(0).getAmount()).isEqualByComparingTo("60.00");
        assertThat(revenue.get(0).getPeriods()).isEqualByComparingTo("2");
        assertThat(revenue.get(0).getTariff().id()).isEqualTo(monthly.getId());
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
                .map(AccountBalance::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private java.util.List<Revenue> revenueFor(Customer customer) {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        return revenues.getTurnover(from, to).stream()
                .filter(r -> r.getCustomer() != null
                        && r.getCustomer().id().equals(customer.getId()))
                .toList();
    }
}