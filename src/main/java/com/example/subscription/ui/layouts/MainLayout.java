package com.example.subscription.ui.layouts;

import com.example.subscription.domain.catalogs.Customer;
import com.example.subscription.domain.catalogs.Tariff;
import com.example.subscription.domain.documents.Payment;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.registers.AccountBalance;
import com.example.subscription.domain.registers.Revenue;
import su.onno.ui.Layout;
import su.onno.ui.LayoutSpec;
import su.onno.ui.NavStyle;

import org.springframework.stereotype.Component;

/**
 * The application's navigation shell.
 *
 * <p>UI structure is authored here as a bean — sections, icons, branding — never as
 * annotations on the domain classes. An entity shows in the sidebar only because a section
 * lists it here; onno does not auto-populate the nav from {@code @Catalog}/{@code @Document}
 * metadata.</p>
 *
 * <p>Two sections are declared:</p>
 * <ul>
 *   <li><b>Sales</b> — the day-to-day working set: customers and tariffs (master data) plus
 *       payments and subscriptions (the two documents that drive the balance and revenue
 *       registers).</li>
 *   <li><b>Reports</b> — read-only views into the two accumulation registers.</li>
 * </ul>
 *
 * <p>The home {@link com.example.subscription.ui.pages.DashboardPage} needs no entry here:
 * a {@code Page} registered at route {@code "/"} is added to the sidebar automatically, so
 * declaring it again would create a duplicate link.</p>
 */
@Component
public class MainLayout implements Layout {

    @Override
    public void configure(LayoutSpec layout) {
        // Shell chrome: sidebar navigation with the app's display name.
        layout.shell()
                .nav(NavStyle.SIDEBAR)
                .brand("Subscriptions");

        // Sales: the entities a manager touches every day. Order within the section follows
        // the natural flow — set up master data, then record documents against it.
        layout.section("Sales")
                .order(0)
                .icon("users")
                .catalog(Customer.class)
                .catalog(Tariff.class)
                .document(Payment.class)
                .document(Subscription.class);

        // Reports: the registers, read-only by nature — they are written by posting, never
        // edited directly. Sorted after Sales so the working set stays on top.
        layout.section("Reports")
                .order(10)
                .icon("chart-column")
                .register(AccountBalance.class)
                .register(Revenue.class);
    }
}