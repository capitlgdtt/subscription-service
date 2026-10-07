package com.example.subscription;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.CustomerRepository;
import com.example.subscription.repositories.SubscriptionRepository;
import com.example.subscription.repositories.TariffRepository;
import com.example.subscription.services.SubscriptionService;
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
 * Verifies {@link SubscriptionService#cancel(UUID, String)} — the flow behind the
 * "Cancel subscription" action on both the row and the detail form.
 *
 * <p>The action handler in {@code SubscriptionView} is a thin adapter that reads the reason
 * from the modal form and delegates here; testing the service exercises the whole
 * cancellation contract without reaching into the UI layer.</p>
 *
 * <p>Traces to the additional-functionality requirement on an {@code ActionSpec} that
 * cancels a subscription with a reason, and to the Level 2 rule that a cancelled
 * subscription behaves as a terminal record.</p>
 */
@Tag("level-2")
class SubscriptionCancelActionTest extends AbstractIntegrationTest {

    @Autowired CustomerRepository customers;
    @Autowired TariffRepository tariffs;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired SubscriptionService subscriptionService;

    @Test
    @DisplayName("[Level 2 · action] Cancel sets the status, stores the reason and persists both")
    void cancel_setsStatusAndReason() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Ref<Customer> customer = newCustomer("Acme-" + tag);
        Ref<Tariff> tariff = newTariff("Tariff-" + tag, "30.00", 30);

        // The save goes through the document validation pipeline — a subscription with no
        // lines would be rejected by the lines-required rule before we could cancel it.
        Subscription sub = new Subscription();
        sub.setCustomer(customer);
        sub.setStatus(SubscriptionStatus.DRAFT);
        sub.setStartDate(LocalDate.now());
        SubscriptionLine line = new SubscriptionLine();
        line.setTariff(tariff);
        line.setPeriods(1);
        sub.getLines().add(line);
        Subscription saved = subscriptions.save(sub);

        Subscription cancelled = subscriptionService.cancel(
                Objects.requireNonNull(saved.getId()),
                "Customer changed their mind");

        assertThat(cancelled).isNotNull();
        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(cancelled.getCancelReason()).isEqualTo("Customer changed their mind");

        // Re-read from the database: the change is persisted, not just returned.
        Subscription reloaded = subscriptions.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(reloaded.getCancelReason()).isEqualTo("Customer changed their mind");
    }

    @Test
    @DisplayName("[Level 2 · action] Cancelling a non-existent subscription returns null and does not throw")
    void cancel_unknownId_returnsNull() {
        assertThat(subscriptionService.cancel(UUID.randomUUID(), "No such subscription"))
                .isNull();
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
}