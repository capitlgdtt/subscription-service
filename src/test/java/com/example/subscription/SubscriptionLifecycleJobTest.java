package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Payment;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.PaymentMethod;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.jobs.SubscriptionLifecycleJob;
import com.example.subscription.repositories.CustomerRepository;
import com.example.subscription.repositories.PaymentRepository;
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
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the date-driven lifecycle transitions performed by
 * {@link SubscriptionLifecycleJob}.
 *
 * <p>The job's {@code execute()} method is invoked directly — no scheduler or JobRunr
 * infrastructure is involved. Each test sets up a subscription in the required pre-state,
 * runs one pass, and re-loads the document to assert the post-state. Fixtures are UUID-tagged
 * so tests sharing the container do not see each other's data.</p>
 *
 * <p>Traces to the Level 2 requirement on the scheduled job: subscriptions transition to
 * Active and Expired by date, and a cancelled subscription is never touched.</p>
 */
@Tag("level-2")
class SubscriptionLifecycleJobTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired PaymentRepository payments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired PostingService posting;
    @Autowired SubscriptionLifecycleJob job;

    @Test
    @DisplayName("[Level 2 · job] A posted subscription whose start date has arrived becomes Active")
    void postedDraft_becomesActiveWhenStartDateArrives() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Ref<Customer> customer = newCustomer("Acme-" + tag);
        Ref<Tariff> monthly = newTariff("Monthly-" + tag, "30.00", 30);
        fundAccount(customer, "100.00");

        Subscription sub = newSubscription(customer, monthly, 1,
                LocalDate.now().minusDays(1));
        Subscription saved = subscriptions.save(sub);
        posting.post(saved);

        // Pre-condition: DRAFT is not a status posting changes — it just sets the posted flag.
        assertThat(reload(saved).getStatus()).isEqualTo(SubscriptionStatus.DRAFT);
        assertThat(reload(saved).isPosted()).isTrue();

        job.execute();

        assertThat(reload(saved).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("[Level 2 · job] An active subscription whose end date has passed becomes Expired")
    void active_becomesExpiredWhenEndDatePasses() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Ref<Customer> customer = newCustomer("Acme-" + tag);
        Ref<Tariff> monthly = newTariff("Monthly-" + tag, "30.00", 30);
        fundAccount(customer, "100.00");

        // Start 60 days ago, tariff lasts 30 days → endDate is 30 days in the past.
        Subscription sub = newSubscription(customer, monthly, 1,
                LocalDate.now().minusDays(60));
        Subscription saved = subscriptions.save(sub);
        posting.post(saved);

        // Put the document in the pre-state the job expects for the second transition,
        // rather than wait for two job passes in the same test.
        saved.setStatus(SubscriptionStatus.ACTIVE);
        subscriptions.save(saved);

        job.execute();

        assertThat(reload(saved).getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("[Level 2 · job] An unposted draft is left alone even when its start date has arrived")
    void unpostedDraft_isNotAdvanced() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Ref<Customer> customer = newCustomer("Acme-" + tag);
        Ref<Tariff> monthly = newTariff("Monthly-" + tag, "30.00", 30);
        // No funding: the subscription cannot be posted.

        Subscription sub = newSubscription(customer, monthly, 1,
                LocalDate.now().minusDays(1));
        Subscription saved = subscriptions.save(sub);
        // Deliberately not posted.

        job.execute();

        // Nothing was drawn from the account — the subscription must not look "running".
        assertThat(reload(saved).getStatus()).isEqualTo(SubscriptionStatus.DRAFT);
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
    private Subscription newSubscription(Ref<Customer> customer, Ref<Tariff> tariff,
                                         int periods, LocalDate startDate) {
        Subscription sub = new Subscription();
        sub.setCustomer(customer);
        sub.setStatus(SubscriptionStatus.DRAFT);
        sub.setStartDate(startDate);
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(tariff);
        line.setPeriods(periods);
        sub.getLines().add(line);
        return sub;
    }

    @SuppressWarnings("SameParameterValue")
    private void fundAccount(Ref<Customer> customer, String amount) {
        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setAmount(new BigDecimal(amount));
        payment.setMethod(PaymentMethod.CARD);
        payments.save(payment);
        posting.post(payment);
    }

    private Subscription reload(Subscription subscription) {
        return subscriptions.findById(
                Objects.requireNonNull(subscription.getId())).orElseThrow();
    }
}