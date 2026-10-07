package com.example.subscription.ui.views;

import com.example.subscription.domain.documents.Subscription;
import com.example.subscription.domain.documents.SubscriptionLine;
import com.example.subscription.domain.enumerations.SubscriptionStatus;
import com.example.subscription.services.SubscriptionService;
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
 * are needed for the action to be reachable from either surface. The action delegates to
 * {@link SubscriptionService}, which holds the actual cancellation logic — the view is
 * only the UI surface.</p>
 *
 * <p><b>Derived columns are hidden from the form.</b> {@code endDate} and {@code total}
 * are recomputed by {@code Subscription.beforeWrite()} on every save; showing them as
 * editable inputs would invite a user to type a value that is immediately overwritten.
 * They remain visible in the list, where they are informational.</p>
 *
 * <p><b>Cancellation reason is visible on the form.</b> onno in this version has no
 * conditional field visibility — a field is either shown or hidden on the single editable
 * surface. {@code cancelReason} therefore stays on the form so that a cancelled
 * subscription shows its audit reason when opened; it is normally populated by the cancel
 * action's modal dialog, not typed by hand.</p>
 */
@Component
public class SubscriptionView implements EntityView<Subscription> {

    private final SubscriptionService subscriptionService;

    public SubscriptionView(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public Class<Subscription> entity() {
        return Subscription.class;
    }

    @Override
    public void list(ListSpec<Subscription> list) {
        list.columns(Subscription::getNumber, Subscription::getDate, Subscription::getCustomer,
                        Subscription::getStatus, Subscription::getStartDate, Subscription::getEndDate,
                        Subscription::getTotal, Subscription::isPosted)
                .label(Subscription::getTotal, "Total")
                .label(Subscription::getStartDate, "Start")
                .label(Subscription::getEndDate, "End")
                .sortBy(Subscription::getDate, true);

        list.filter(Subscription::getStatus).label("Status").multiOptions();
    }

    @Override
    public void fields(EntityConfigBuilder<Subscription> f) {
        f.field(Subscription::getCustomer).order(0).width("half")
                .field(Subscription::getStatus).order(1).width("half")
                .field(Subscription::getStartDate).order(2).width("half")
                .field(Subscription::getEndDate).order(3).width("half")
                .hideInForm()
                .hint("Auto-computed: start date + max line duration.")
                .field(Subscription::getTotal).order(4)
                .hideInForm()
                .format("currency:USD")
                .hint("Auto-computed from line amounts.")
                .field(Subscription::getCancelReason).order(5)
                .widget("textarea")
                .hint("Auto-filled by the cancel action. Can be edited here if needed.");

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
        // Hidden for already-cancelled documents — cancelling twice would overwrite the
        // audit reason with a new one.
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

        // DETAIL scope: same action as a header button on the subscription form.
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

    private ActionResult cancel(UUID id, String reason) {
        return subscriptionService.cancel(id, reason) != null
                ? ActionResult.refresh(ActionToast.success("Subscription cancelled"))
                : ActionResult.toast(ActionToast.warning("Subscription not found"));
    }
}