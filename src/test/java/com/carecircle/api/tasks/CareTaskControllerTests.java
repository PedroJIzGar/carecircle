package com.carecircle.api.tasks;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
