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
        list.columns(Subscription::getNumber, Subscription::getDate, Subscription::getCustomer,
                        Subscription::getStatus, Subscription::getStartDate, Subscription::getEndDate,
                        Subscription::getTotal, Subscription::isPosted)
                .label(Subscription::getTotal, "Total")
                .label(Subscription::getStartDate, "Start")
                .label(Subscription::getEndDate, "End")
                .sortBy(Subscription::getDate, true);
    }

    @Override
    public void fields(EntityConfigBuilder<Subscription> f) {
        f.field(Subscription::getCustomer).order(0).width("half")
                .field(Subscription::getStatus).order(1).width("half")
                .field(Subscription::getStartDate).order(2).width("half")
                .field(Subscription::getEndDate).order(3).width("half").hideInForm()
                .hint("Auto-computed: start date + max line duration.")
                .field(Subscription::getTotal).order(4).hideInForm().format("currency:USD")
                .hint("Auto-computed from line amounts.")
                .field(Subscription::getCancelReason).order(5).widget("textarea")
                .hint("Set when the subscription is cancelled.");

        f.rowField(Subscription::getLines, SubscriptionLine::getTariff).label("Tariff");
        f.rowField(Subscription::getLines, SubscriptionLine::getPeriods).label("Periods");
        f.rowField(Subscription::getLines, SubscriptionLine::getPrice)
                .label("Price").format("currency:USD");
        f.rowField(Subscription::getLines, SubscriptionLine::getAmount)
                .label("Amount").format("currency:USD");
    }

    @Override
    public void actions(ActionSpec a) {
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
        return subscriptions.findById(id).map(s -> {
            s.setStatus(SubscriptionStatus.CANCELLED);
            s.setCancelReason(reason);
            subscriptions.save(s);
            return ActionResult.refresh(ActionToast.success("Subscription cancelled"));
        }).orElseGet(() ->
                ActionResult.toast(ActionToast.warning("Subscription not found")));
    }
}