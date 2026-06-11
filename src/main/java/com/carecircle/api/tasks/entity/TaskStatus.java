package com.carecircle.api.tasks.entity;

/**
 * Lifecycle state for a family coordination task.
 */
public enum TaskStatus {
    /**
     * Task is pending and visible in active work lists.
     */
    OPEN,

    /**
     * Task has been completed by a care circle member.
     */
    COMPLETED,

    /**
     * Task is no longer needed but remains in history.
     */
    CANCELLED
}
