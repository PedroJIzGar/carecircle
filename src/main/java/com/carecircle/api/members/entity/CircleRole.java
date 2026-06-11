package com.carecircle.api.members.entity;

/**
 * Family role granted to a user inside one care circle.
 *
 * <p>These roles are scoped to a circle and must not be stored as User.globalRole.</p>
 */
public enum CircleRole {
    MAIN_CAREGIVER,
    COLLABORATOR,
    OBSERVER
}
