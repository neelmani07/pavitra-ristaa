package com.pavitraristaa.notifications.service;

import com.pavitraristaa.admin.event.AppealResolvedEvent;
import com.pavitraristaa.admin.event.ReportResolvedEvent;
import com.pavitraristaa.admin.event.SupportTicketResolvedEvent;
import com.pavitraristaa.admin.event.UserStatusChangedEvent;
import com.pavitraristaa.admin.event.VerificationDecidedEvent;
import com.pavitraristaa.auth.entity.AccountStatus;
import com.pavitraristaa.connections.entity.Interest;
import com.pavitraristaa.connections.entity.Match;
import com.pavitraristaa.connections.event.InterestReceivedEvent;
import com.pavitraristaa.connections.event.MatchActivatedEvent;
import com.pavitraristaa.notifications.entity.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns domain events from other modules into notification rows. None of those modules know this listener
 * exists - each just publishes its own event via ApplicationEventPublisher (the same pattern messaging already
 * uses to react to connections' match events), so notifications depends on connections and admin for their
 * event/entity types, but nothing depends back on notifications. Runs @EventListener (synchronous, same
 * transaction as the publisher) so a notification is never lost to a later failure, matching
 * ConversationService's onMatchActivated/onMatchEnded listeners.
 */
@Component
class NotificationEventListener {

    private final NotificationPublisher notificationPublisher;

    NotificationEventListener(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @EventListener
    @Transactional
    public void onInterestReceived(InterestReceivedEvent event) {
        Interest interest = event.interest();
        notificationPublisher.notify(
                interest.getReceiver(),
                NotificationType.INTEREST_RECEIVED,
                "New interest received",
                "Someone is interested in connecting with you.",
                "interest",
                interest.getId());
    }

    @EventListener
    @Transactional
    public void onMatchActivated(MatchActivatedEvent event) {
        Match match = event.match();
        String title = "It's a match!";
        String body = "You have a new match.";
        notificationPublisher.notify(match.getUserA(), NotificationType.MATCH_CREATED, title, body, "match", match.getId());
        notificationPublisher.notify(match.getUserB(), NotificationType.MATCH_CREATED, title, body, "match", match.getId());
    }

    @EventListener
    @Transactional
    public void onVerificationDecided(VerificationDecidedEvent event) {
        if (event.approved()) {
            notificationPublisher.notify(
                    event.targetUser(), NotificationType.VERIFICATION_APPROVED,
                    "Verification approved", "Your profile verification has been approved.", null, null);
        } else {
            notificationPublisher.notify(
                    event.targetUser(), NotificationType.VERIFICATION_REJECTED,
                    "Verification rejected", "Your profile verification request was not approved.", null, null);
        }
    }

    @EventListener
    @Transactional
    public void onReportResolved(ReportResolvedEvent event) {
        notificationPublisher.notify(
                event.reporter(), NotificationType.REPORT_RESOLVED,
                "Report update", "A report you submitted has been reviewed.", null, null);
    }

    @EventListener
    @Transactional
    public void onUserStatusChanged(UserStatusChangedEvent event) {
        if (AccountStatus.SUSPENDED.name().equals(event.newStatus())) {
            notificationPublisher.notify(
                    event.targetUser(), NotificationType.ACCOUNT_SUSPENDED,
                    "Account suspended", "Your account has been suspended. Contact support for details.", null, null);
        } else if (AccountStatus.ACTIVE.name().equals(event.newStatus())) {
            notificationPublisher.notify(
                    event.targetUser(), NotificationType.ACCOUNT_REACTIVATED,
                    "Account reactivated", "Your account is active again.", null, null);
        }
    }

    @EventListener
    @Transactional
    public void onSupportTicketResolved(SupportTicketResolvedEvent event) {
        notificationPublisher.notify(
                event.ticketOwner(), NotificationType.SUPPORT_TICKET_RESOLVED,
                "Support ticket resolved", "Your support ticket has been resolved.", null, null);
    }

    @EventListener
    @Transactional
    public void onAppealResolved(AppealResolvedEvent event) {
        if (event.approved()) {
            notificationPublisher.notify(
                    event.appellant(), NotificationType.APPEAL_RESOLVED,
                    "Appeal approved", "Your appeal has been approved.", null, null);
        } else {
            notificationPublisher.notify(
                    event.appellant(), NotificationType.APPEAL_RESOLVED,
                    "Appeal rejected", "Your appeal was reviewed and was not approved.", null, null);
        }
    }
}
