package com.pavitraristaa.admin.event;

import com.pavitraristaa.auth.entity.UserAccount;

/** Published when an admin resolves or dismisses a report; notifies the original reporter. */
public record ReportResolvedEvent(UserAccount reporter, String status) {
}
