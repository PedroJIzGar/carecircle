package com.carecircle.api.tasks.entity;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Non-clinical task used by family members to coordinate daily care.
 *
 * <p>Tasks are intentionally scoped to family coordination. They must not be
 * used to diagnose, recommend treatments, or change medication.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareTask {

    /**
     * Internal task identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the task belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * Short task title shown in lists.
     */
    @Column(name = "title", nullable = false, length = 160)
    private String title;

    /**
     * Optional task details for family coordination.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Current task lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status = TaskStatus.OPEN;

    /**
     * Relative task urgency.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private TaskPriority priority = TaskPriority.NORMAL;

    /**
     * Optional due date and time.
     */
    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    /**
     * Optional user currently responsible for the task.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    /**
     * User that created the task.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /**
     * Completion timestamp, present only for completed tasks.
     */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /**
     * User that completed the task, present only for completed tasks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private User completedByUser;

    /**
     * Entity creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Creates an open task in a care circle.
     *
     * @param careCircle circle that owns the task.
     * @param title short family-visible task title.
     * @param createdByUser user creating the task.
     */
    public CareTask(CareCircle careCircle, String title, User createdByUser) {
        this.careCircle = careCircle;
        this.title = title;
        this.createdByUser = createdByUser;
        this.status = TaskStatus.OPEN;
        this.priority = TaskPriority.NORMAL;
    }

    /**
     * Ensures timestamps are present for new tasks created through JPA.
     */
    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Updates the modification timestamp before every JPA update.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
