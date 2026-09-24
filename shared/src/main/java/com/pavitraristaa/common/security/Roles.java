package com.pavitraristaa.common.security;

/** SpEL expression constants for @PreAuthorize, matching the roles seeded in the role table. */
public final class Roles {

    public static final String MODERATOR_OR_ABOVE = "hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')";
    public static final String ADMIN_OR_ABOVE = "hasAnyRole('ADMIN','SUPER_ADMIN')";

    private Roles() {
    }
}
