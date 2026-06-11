package com.carecircle.api.tasks.entity;

/**
 * Relative urgency for a family coordination task.
 */
public enum TaskPriority {
    /**
     * Low urgency task.
     */
    LOW,

    /**
     * Default task priority.
     */
    NORMAL,

    /**
     * High urgency task that should stand out in lists.
     */
    HIGH
}
