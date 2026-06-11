package com.carecircle.api.tasks;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.tasks.entity.CareTask;
import com.carecircle.api.tasks.entity.TaskPriority;
import com.carecircle.api.tasks.entity.TaskStatus;
import com.carecircle.api.tasks.repository.CareTaskRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CareTaskControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareCircleRepository careCircleRepository;

    @Autowired
    private ElderProfileRepository elderProfileRepository;

    @Autowired
    private CircleMemberRepository circleMemberRepository;

    @Autowired
    private CareTaskRepository careTaskRepository;

    @Test
    void updateTaskUpdatesEditableFieldsWhenCurrentUserIsCollaborator() throws Exception {
        User mainCaregiver = createUser("task-update-main", "Task Update Main");
        User collaborator = createUser("task-update-collaborator", "Task Update Collaborator");
        User assignee = createUser("task-update-assignee", "Task Update Assignee");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        circleMemberRepository.save(new CircleMember(careCircle, assignee, CircleRole.OBSERVER));

        CareTask task = careTaskRepository.save(new CareTask(careCircle, "Old title", mainCaregiver));
        OffsetDateTime dueAt = OffsetDateTime.now().plusDays(2).withNano(0);

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), task.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  New title  ",
                                  "description": "  Updated details  ",
                                  "priority": "HIGH",
                                  "dueAt": "%s",
                                  "assignedToUserId": "%s"
                                }
                                """.formatted(dueAt, assignee.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId().toString()))
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("Updated details"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignedToUserId").value(assignee.getId().toString()))
                .andExpect(jsonPath("$.assignedToFullName").value("Task Update Assignee"));

        assertThat(careTaskRepository.findById(task.getId()))
                .isPresent()
                .get()
                .satisfies(updatedTask -> {
                    assertThat(updatedTask.getTitle()).isEqualTo("New title");
                    assertThat(updatedTask.getDescription()).isEqualTo("Updated details");
                    assertThat(updatedTask.getPriority()).isEqualTo(TaskPriority.HIGH);
                    assertThat(updatedTask.getDueAt()).isEqualTo(dueAt);
                    assertThat(updatedTask.getAssignedToUser().getId()).isEqualTo(assignee.getId());
                });
    }

    @Test
    void updateTaskClearsOptionalFields() throws Exception {
        User mainCaregiver = createUser("task-clear-main", "Task Clear Main");
        User assignee = createUser("task-clear-assignee", "Task Clear Assignee");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task clear family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Clear Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, assignee, CircleRole.COLLABORATOR));

        CareTask task = new CareTask(careCircle, "Task with optional fields", mainCaregiver);
        task.setDescription("Existing description");
        task.setDueAt(OffsetDateTime.now().plusDays(1).withNano(0));
        task.setAssignedToUser(assignee);
        CareTask savedTask = careTaskRepository.save(task);

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), savedTask.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clearDescription": true,
                                  "clearDueAt": true,
                                  "clearAssignment": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTask.getId().toString()));

        assertThat(careTaskRepository.findById(savedTask.getId()))
                .isPresent()
                .get()
                .satisfies(updatedTask -> {
                    assertThat(updatedTask.getDescription()).isNull();
                    assertThat(updatedTask.getDueAt()).isNull();
                    assertThat(updatedTask.getAssignedToUser()).isNull();
                });
    }

    @Test
    void updateTaskRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("task-update-observer-main", "Task Update Observer Main");
        User observer = createUser("task-update-observer-user", "Task Update Observer User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update observer family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Observer Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        CareTask task = careTaskRepository.save(new CareTask(careCircle, "Observer blocked task", mainCaregiver));

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), task.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Observer update"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can update care circle tasks."));
    }

    @Test
    void updateTaskReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("task-update-outside-main", "Task Update Outside Main");
        User outsideUser = createUser("task-update-outside-user", "Task Update Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CareTask task = careTaskRepository.save(new CareTask(careCircle, "Private update task", mainCaregiver));

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), task.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Outside update"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateTaskReturnsNotFoundWhenTaskBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("task-update-other-main", "Task Update Other Main");
        User otherMainCaregiver = createUser("task-update-other-owner", "Task Update Other Owner");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update current family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Current Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Task update other family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Task Update Other Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherMainCaregiver, CircleRole.MAIN_CAREGIVER));
        CareTask otherTask = careTaskRepository.save(new CareTask(otherCircle, "Other circle update task", otherMainCaregiver));

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), otherTask.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Wrong circle update"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found."));
    }

    @Test
    void updateTaskReturnsNotFoundWhenAssignedUserIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("task-update-assigned-main", "Task Update Assigned Main");
        User outsideUser = createUser("task-update-assigned-outside", "Task Update Assigned Outside");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update assigned family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Assigned Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CareTask task = careTaskRepository.save(new CareTask(careCircle, "Assign update task", mainCaregiver));

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), task.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": "%s"
                                }
                                """.formatted(outsideUser.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assigned user not found in care circle."));
    }

    @Test
    void updateTaskRejectsCompletedTask() throws Exception {
        User mainCaregiver = createUser("task-update-completed-main", "Task Update Completed Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update completed family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Completed Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CareTask task = new CareTask(careCircle, "Completed update task", mainCaregiver);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(OffsetDateTime.now().withNano(0));
        CareTask savedTask = careTaskRepository.save(task);

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), savedTask.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Should fail"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Only open tasks can be updated."));
    }

    @Test
    void updateTaskValidatesBlankTitle() throws Exception {
        User mainCaregiver = createUser("task-update-invalid-main", "Task Update Invalid Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update invalid family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Invalid Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CareTask task = careTaskRepository.save(new CareTask(careCircle, "Invalid update task", mainCaregiver));

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), task.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Task title must not be blank."));
    }

    @Test
    void updateTaskRejectsConflictingClearAndSet() throws Exception {
        User mainCaregiver = createUser("task-update-conflict-main", "Task Update Conflict Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task update conflict family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Update Conflict Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CareTask task = careTaskRepository.save(new CareTask(careCircle, "Conflict update task", mainCaregiver));

        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", careCircle.getId(), task.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "New description",
                                  "clearDescription": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("description cannot be set and cleared in the same request."));
    }

    @Test
    void updateTaskRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/tasks/{taskId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unauthenticated update"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTasksReturnsCircleTasksForObserverOrderedByStatusAndDueDate() throws Exception {
        User mainCaregiver = createUser("task-list-main", "Task List Main");
        User collaborator = createUser("task-list-collaborator", "Task List Collaborator");
        User observer = createUser("task-list-observer", "Task List Observer");
        User otherMainCaregiver = createUser("task-list-other-main", "Task List Other Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task list family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task List Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Other task list family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Other Task List Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherMainCaregiver, CircleRole.MAIN_CAREGIVER));

        OffsetDateTime dueAt = OffsetDateTime.now().plusDays(1).withNano(0);

        CareTask completedTask = new CareTask(careCircle, "Completed task", mainCaregiver);
        completedTask.setStatus(TaskStatus.COMPLETED);
        completedTask.setCompletedAt(OffsetDateTime.now().withNano(0));
        completedTask.setCompletedByUser(collaborator);
        completedTask.setDueAt(dueAt.minusHours(2));
        careTaskRepository.save(completedTask);

        careTaskRepository.save(new CareTask(careCircle, "Open task without due date", mainCaregiver));

        CareTask dueSoonTask = new CareTask(careCircle, "Open task due soon", mainCaregiver);
        dueSoonTask.setDueAt(dueAt.minusHours(1));
        dueSoonTask.setAssignedToUser(collaborator);
        careTaskRepository.save(dueSoonTask);

        careTaskRepository.save(new CareTask(otherCircle, "Other circle task", otherMainCaregiver));

        mockMvc.perform(get("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Open task due soon"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].assignedToUserId").value(collaborator.getId().toString()))
                .andExpect(jsonPath("$[1].title").value("Open task without due date"))
                .andExpect(jsonPath("$[1].status").value("OPEN"))
                .andExpect(jsonPath("$[2].title").value("Completed task"))
                .andExpect(jsonPath("$[2].status").value("COMPLETED"))
                .andExpect(jsonPath("$[?(@.title == 'Other circle task')]", hasSize(0)));
    }

    @Test
    void listTasksReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("task-list-outside-main", "Task List Outside Main");
        User outsideUser = createUser("task-list-outside-user", "Task List Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task list outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task List Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        careTaskRepository.save(new CareTask(careCircle, "Private task", mainCaregiver));

        mockMvc.perform(get("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listTasksReturnsNotFoundWhenCurrentMembershipWasRemoved() throws Exception {
        User mainCaregiver = createUser("task-list-removed-main", "Task List Removed Main");
        User removedUser = createUser("task-list-removed-user", "Task List Removed User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task list removed family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task List Removed Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CircleMember removedMembership = new CircleMember(careCircle, removedUser, CircleRole.COLLABORATOR);
        removedMembership.setStatus(CircleMemberStatus.REMOVED);
        circleMemberRepository.save(removedMembership);
        careTaskRepository.save(new CareTask(careCircle, "Removed member hidden task", mainCaregiver));

        mockMvc.perform(get("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(removedUser.getSupabaseUserId())
                                .claim("email", removedUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listTasksRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/tasks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTaskCreatesOpenTaskWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("task-create-main", "Task Main");
        User collaborator = createUser("task-create-collaborator", "Task Collaborator");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task create family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Create Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        OffsetDateTime dueAt = OffsetDateTime.now().plusDays(1).withNano(0);

        mockMvc.perform(post("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Buy groceries  ",
                                  "description": "  Pick up fruit and water  ",
                                  "priority": "HIGH",
                                  "dueAt": "%s",
                                  "assignedToUserId": "%s"
                                }
                                """.formatted(dueAt, collaborator.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careCircleId").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.title").value("Buy groceries"))
                .andExpect(jsonPath("$.description").value("Pick up fruit and water"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignedToUserId").value(collaborator.getId().toString()))
                .andExpect(jsonPath("$.assignedToFullName").value("Task Collaborator"))
                .andExpect(jsonPath("$.createdByUserId").value(mainCaregiver.getId().toString()));

        assertThat(careTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.getTitle()).isEqualTo("Buy groceries");
                    assertThat(task.getDescription()).isEqualTo("Pick up fruit and water");
                    assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
                    assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
                    assertThat(task.getAssignedToUser().getId()).isEqualTo(collaborator.getId());
                });
    }

    @Test
    void createTaskDefaultsPriorityAndAllowsCollaborator() throws Exception {
        User mainCaregiver = createUser("task-collab-main", "Task Collab Main");
        User collaborator = createUser("task-collab-user", "Task Collab User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task collaborator family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Collaborator Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        mockMvc.perform(post("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Call pharmacy"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Call pharmacy"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("NORMAL"))
                .andExpect(jsonPath("$.createdByUserId").value(collaborator.getId().toString()));
    }

    @Test
    void createTaskRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("task-observer-main", "Task Observer Main");
        User observer = createUser("task-observer-user", "Task Observer User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task observer family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Observer Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        mockMvc.perform(post("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Prepare documents"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can create care circle tasks."));
    }

    @Test
    void createTaskReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("task-outside-main", "Task Outside Main");
        User outsideUser = createUser("task-outside-user", "Task Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Outside task"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createTaskReturnsNotFoundWhenAssignedUserIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("task-assigned-main", "Task Assigned Main");
        User outsideUser = createUser("task-assigned-outside", "Task Assigned Outside");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Task assigned family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Task Assigned Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/tasks", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Assign outside",
                                  "assignedToUserId": "%s"
                                }
                                """.formatted(outsideUser.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assigned user not found in care circle."));
    }

    @Test
    void createTaskValidatesTitle() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/tasks", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "task-invalid-title-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTaskRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/tasks", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unauthenticated task"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String prefix, String fullName) {
        User user = new User(
                UUID.randomUUID().toString(),
                prefix + "-" + UUID.randomUUID() + "@example.com"
        );
        user.setFullName(fullName);
        return userRepository.save(user);
    }
}
