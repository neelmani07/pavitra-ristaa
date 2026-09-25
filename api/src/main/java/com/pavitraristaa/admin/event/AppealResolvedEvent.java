package com.pavitraristaa.admin.event;

import com.pavitraristaa.auth.entity.UserAccount;

/** Published when an admin approves or rejects an appeal; notifies the appellant. */
public record AppealResolvedEvent(UserAccount appellant, boolean approved) {
}
