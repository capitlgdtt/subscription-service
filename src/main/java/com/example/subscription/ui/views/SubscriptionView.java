package com.example.subscription.ui.views;

import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.repositories.SubscriptionRepository;
import su.onno.ui.ActionResult;
import su.onno.ui.ActionScope;
import su.onno.ui.ActionSpec;
import su.onno.ui.ActionToast;
import su.onno.ui.EntityConfigBuilder;
import su.onno.ui.EntityView;
import su.onno.ui.ListSpec;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * UI shape of the {@link Subscription} document — the richest entity view in the service.
 *
 * <p>Besides list columns, form layout, and tabular-section hints, this view declares a
 * <b>cancel action</b> in two scopes: as a row action on the list and as a detail-header
 * action on the form. onno treats those scopes as separate buttons, so both declarations
 * are needed for the action to be reachable from either surface.</p>
 *
 * <p><b>Cancellation only changes the status.</b> It does not delete the document, and it
 * does not reverse any register movements a previous posting may have written. The intended
 * flow is to cancel <em>before</em> posting: an unposted draft's status becomes
 * {@link SubscriptionStatus#CANCELLED} and, if it is later posted, no movements are written
 * (see {@code Subscription.handlePosting}). Once a subscription has been posted, its
 * movements are frozen — cancelling the document afterwards leaves them in place, which is
 * the correct behaviour for an accounting ledger.</p>
 *
 * <p><b>Two business columns are hidden from the form.</b> {@code endDate} and {@code total}
 * are derived by {@code Subscription.beforeWrite()} on every save; showing them as editable
 * inputs would invite a user to type a value that is immediately overwritten. They remain
 * visible in the list, where they are informational.</p>
 */
@Component
public class SubscriptionView implements EntityView<Subscription> {

    private final SubscriptionRepository subscriptions;

    public SubscriptionView(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public Class<Subscription> entity() {
        return Subscription.class;
    }

    @Override
    public void list(ListSpec<Subscription> list) {
        // Newest first — subscriptions are dated documents, and the recent activity is
        // what a manager wants to see at the top.
        list.columns(Subscription::getNumber, Subscription::getDate, Subscription::getCustomer,
                        Subscription::getStatus, Subscription::getStartDate, Subscription::getEndDate,
                        Subscription::getTotal, Subscription::isPosted)
                .label(Subscription::getTotal, "Total")
                .label(Subscription::getStartDate, "Start")
                .label(Subscription::getEndDate, "End")
                .sortBy(Subscription::getDate, true);

        // Registers the status enum with the metadata layer, which lets the dashboard
        // chart group subscriptions by status and render the coloured pill labels.
        list.filter(Subscription::getStatus).label("Status").multiOptions();
    }

    @Override
    public void fields(EntityConfigBuilder<Subscription> f) {
        // Paired short fields — customer/status, start/end — then the auto-computed total
        // and the free-text reason. The tabular section below gets its own column hints.
        f.field(Subscription::getCustomer).order(0).width("half")
                .field(Subscription::getStatus).order(1).width("half")
                .field(Subscription::getStartDate).order(2).width("half")
                .field(Subscription::getEndDate).order(3).width("half").hideInForm()
                .hint("Auto-computed: start date + max line duration.")
                .field(Subscription::getTotal).order(4).hideInForm().format("currency:USD")
                .hint("Auto-computed from line amounts.")
                .field(Subscription::getCancelReason).order(5).widget("textarea")
                .hint("Set when the subscription is cancelled.");

        // Tabular-section column hints: "<section>.<field>" style via rowField().
        f.rowField(Subscription::getLines, SubscriptionLine::getTariff).label("Tariff");
        f.rowField(Subscription::getLines, SubscriptionLine::getPeriods).label("Periods");
        f.rowField(Subscription::getLines, SubscriptionLine::getPrice)
                .label("Price").format("currency:USD");
        f.rowField(Subscription::getLines, SubscriptionLine::getAmount)
                .label("Amount").format("currency:USD");
    }

    @Override
    public void actions(ActionSpec a) {
        // ROW scope: the "Cancel subscription" button appears inline on the list row.
        // The action is hidden for already-cancelled subscriptions — cancelling twice is
        // meaningless and would only overwrite the audit reason.
        //
        // The modal form's labels are chosen to avoid colliding with the framework's own
        // dialog chrome: the built-in close button is already labelled "Cancel", so the
        // submit button reads "Cancel subscription" and the dismiss button "Back".
        a.action("cancel").scope(ActionScope.ROW).icon("ban").label("Cancel subscription")
                .visibleWhen(row -> {
                    SubscriptionStatus st = row.enumValue("status", SubscriptionStatus.class);
                    return st != SubscriptionStatus.CANCELLED;
                })
                .form(f -> {
                    f.title("Cancel subscription")
                            .submitLabel("Cancel subscription")
                            .cancelLabel("Back");
                    f.input("reason").label("Reason")
                            .type(su.onno.ui.InputType.TEXTAREA)
                            .placeholder("Why is this subscription cancelled?")
                            .required();
                })
                .handler(ctx -> cancel(ctx.id(), ctx.input("reason")));

        // DETAIL scope: same action, but as a header button on the subscription's form.
        // Separate declaration because onno does not share actions between scopes.
        a.action("cancelTop").scope(ActionScope.DETAIL).icon("ban").label("Cancel subscription")
                .visibleWhen(row -> {
                    SubscriptionStatus st = row.enumValue("status", SubscriptionStatus.class);
                    return st != SubscriptionStatus.CANCELLED;
                })
                .form(f -> {
                    f.title("Cancel subscription")
                            .submitLabel("Cancel subscription")
                            .cancelLabel("Back");
                    f.input("reason").label("Reason")
                            .type(su.onno.ui.InputType.TEXTAREA)
                            .placeholder("Why is this subscription cancelled?")
                            .required();
                })
                .handler(ctx -> cancel(ctx.id(), ctx.input("reason")));
    }

    /**
     * Sets a subscription's status to {@link SubscriptionStatus#CANCELLED} and stores the
     * reason for the audit trail.
     *
     * <p>Loads the document by id rather than mutating a row payload: the actions above may
     * run on a list row whose projection is not the full document. Saving via the repository
     * also ensures the state change is one atomic write.</p>
     */
    private ActionResult cancel(UUID id, String reason) {
        return subscriptions.findById(id).map(s -> {
            s.setStatus(SubscriptionStatus.CANCELLED);
            s.setCancelReason(reason);
            subscriptions.save(s);
            return ActionResult.refresh(ActionToast.success("Subscription cancelled"));
        }).orElseGet(() ->
                ActionResult.toast(ActionToast.warning("Subscription not found")));
    }
}