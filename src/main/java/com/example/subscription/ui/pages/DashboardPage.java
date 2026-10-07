package com.example.subscription.ui.pages;

import com.example.subscription.domain.documents.Payment;
import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.registers.AccountBalance;
import com.example.subscription.domain.registers.Revenue;
import su.onno.ui.Page;
import su.onno.ui.PageBuilder;

import org.springframework.stereotype.Component;

/**
 * Home dashboard — a single-glance board for the subscription service.
 *
 * <p>This is the application's landing page: it registers at route {@code "/"}, so onno
 * makes it the default surface a signed-in user sees. Unlike the auto-generated entity
 * lists, a {@link Page} is freeform — everything below is composed in code.</p>
 *
 * <p>The board is composed entirely from the domain's own metadata, so every tile and chart
 * reads live from the registers and documents; there is no separate reporting layer:</p>
 * <ul>
 *   <li>a shared {@code timeRange} picker at the top sets the window every other widget reads
 *       from — the tiles and time-bucketed charts all obey it;</li>
 *   <li>four KPI tiles: subscriptions created, revenue recognised, periods sold, and the
 *       current sum of all customer account balances;</li>
 *   <li>four charts: revenue over time, revenue split by tariff, subscriptions by status,
 *       and payments by method;</li>
 *   <li>two recent-document lists — the newest subscriptions and payments.</li>
 * </ul>
 *
 * <p>The two "by tariff" and "by status/method" charts group by a column, not a field:
 * {@code tariffName}, {@code status}, and {@code method} are all column names, and the
 * framework resolves each to its display label automatically (that is why the revenue
 * register carries denormalised {@code tariffName}/{@code customerName} dimensions — see
 * {@link Revenue}).</p>
 */
@Component
public class DashboardPage implements Page {

    @Override
    public String route() {
        return "/";
    }

    @Override
    public void compose(PageBuilder b) {
        b.title("Dashboard");
        b.subtitle("Subscriptions, revenue and account balances at a glance");

        // One shared period picker — every chart and tile below reads from it. Placing it
        // first (order = -100) keeps it visually anchored at the top of the board.
        b.widget("Period").type("timeRange").width("full").order(-100)
                .config("presets", "24h,7d,30d,90d,1y,all")
                .config("default", "30d");

        // ---- KPI row ---------------------------------------------------------------------------

        b.widget("Subscriptions created").type("count").width("1/4").order(0).document(Subscription.class)
                .dateField(Subscription::getDate)
                .config("metric", "count")
                .hint("Subscriptions created during the selected period.");

        b.widget("Revenue (recognised)").type("count").width("1/4").order(1).register(Revenue.class)
                .config("metric", "sum").metricField(Revenue::getAmount)
                .config("currency", "USD")
                .hint("Turnover of the Revenue register for the selected period.");

        b.widget("Periods sold").type("count").width("1/4").order(2).register(Revenue.class)
                .config("metric", "sum").metricField(Revenue::getPeriods)
                .hint("Total subscription periods sold for the selected period.");

        b.widget("Account balance").type("count").width("1/4").order(3).register(AccountBalance.class)
                .config("metric", "sum").metricField(AccountBalance::getAmount)
                .config("currency", "USD")
                .hint("Sum of all customer account balances right now.");

        // ---- Charts ----------------------------------------------------------------------------

        // Time-bucketed revenue: the period column is a timestamp, so the chart groups it
        // into day-sized buckets. Widths are fractional — 2/3 + 1/3 fill the row.
        b.widget("Revenue by day").type("chart").width("2/3").order(10).register(Revenue.class)
                .config("kind", "area")
                .groupBy(Revenue::getPeriod).config("groupByDate", "day")
                .config("metric", "sum").metricField(Revenue::getAmount)
                .config("currency", "USD")
                .hint("Daily recognised revenue over the selected period.");

        // Group by the denormalised name column, not the ref: register widgets do not
        // resolve a ref's label, so grouping by `tariff` would render raw UUIDs.
        b.widget("Revenue by tariff").type("chart").width("1/3").order(11).register(Revenue.class)
                .config("kind", "pie")
                .config("groupBy", "tariffName")
                .config("metric", "sum").metricField(Revenue::getAmount)
                .config("currency", "USD")
                .hint("Which tariffs bring the revenue.");

        b.widget("Subscriptions by status").type("chart").width("1/2").order(12)
                .document(Subscription.class)
                .config("kind", "bar")
                .config("groupBy", "status")
                .config("metric", "count")
                .hint("Where subscriptions sit in their lifecycle.");

        b.widget("Payments by method").type("chart").width("1/2").order(13)
                .document(Payment.class)
                .config("kind", "pie")
                .config("groupBy", "method")
                .config("metric", "count")
                .hint("How customers pay.");

        // ---- Recent documents ------------------------------------------------------------------

        b.widget("Recent subscriptions").type("list").width("1/2").order(20)
                .document(Subscription.class).maxItems(10)
                .config("titleTemplate", "{number} · {customerDisplay}")
                .config("secondaryField", "statusDisplay");

        b.widget("Recent payments").type("list").width("1/2").order(21)
                .document(Payment.class).maxItems(10)
                .config("titleTemplate", "{number} · {customerDisplay}")
                .config("secondaryField", "amount");
    }
}