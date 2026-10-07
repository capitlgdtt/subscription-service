package com.example.subscription.domain.enumerations;

import su.onno.annotations.EnumLabel;
import su.onno.annotations.Enumeration;

/**
 * Lifecycle of a {@link com.example.subscription.domain.documents.Subscription}.
 *
 * <p>The status is meaningful at posting time — {@link #CANCELLED} suppresses all
 * movements — and is advanced automatically by
 * {@link com.example.subscription.jobs.SubscriptionLifecycleJob} as dates pass. The
 * transitions the job performs:</p>
 * <ul>
 *   <li>{@link #DRAFT} → {@link #ACTIVE} once the start date has arrived and the document
 *       has posted;</li>
 *   <li>{@link #ACTIVE} → {@link #EXPIRED} once the end date has passed.</li>
 * </ul>
 *
 * <p>{@link #CANCELLED} is terminal: the job never touches a cancelled document, and its
 * reason is preserved on the record for audit.</p>
 */
@Enumeration(name = "Subscription Statuses", title = "Subscription status")
public enum SubscriptionStatus {
    @EnumLabel(value = "Draft", color = "#6B7280") DRAFT,
    @EnumLabel(value = "Active", color = "#059669") ACTIVE,
    @EnumLabel(value = "Expired", color = "#D97706") EXPIRED,
    @EnumLabel(value = "Cancelled", color = "#DC2626") CANCELLED
}