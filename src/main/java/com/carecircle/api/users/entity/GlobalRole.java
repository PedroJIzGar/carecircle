package com.carecircle.api.users.entity;

/**
 * Application-level role.
 *
 * Circle-specific family roles such as MAIN_CAREGIVER, COLLABORATOR and OBSERVER
 * must be modeled in the membership module, not here.
 */
public enum GlobalRole {
    USER,
    ADMIN,
    PARTNER_USER
}
