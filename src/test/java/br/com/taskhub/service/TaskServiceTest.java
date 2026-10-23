package br.com.taskhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.taskhub.audit.AuditEvent;
import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.Priority;
import br.com.taskhub.entity.Project;
import br.com.taskhub.entity.Status;
import br.com.taskhub.entity.Task;
import br.com.taskhub.entity.User;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.InvalidStatusTransitionException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.TaskMapper;
import br.com.taskhub.repository.ProjectMemberRepository;
import br.com.taskhub.repository.ProjectRepository;
import br.com.taskhub.repository.TaskRepository;
import br.com.taskhub.repository.UserRepository;

@ExtendWith (MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaskService taskService;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    private UUID projectId;
    private UUID taskId;
    private UUID userId;
    private Project project;
    private Task task;
    private TaskResponseDTO response;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();
        project = new Project("Projeto", "Descrição");
        task = new Task("Tarefa", "Descrição", Priority.HIGH, LocalDate.now().plusDays(1), project);
        ReflectionTestUtils.setField(task, "id", taskId);
        response = new TaskResponseDTO(taskId, "Tarefa", "Descrição", Status.TODO, Priority.HIGH,
                LocalDate.now().plusDays(1), projectId, null, null);
    }

    private TaskRequestDTO request(UUID assigneeId) {
        return new TaskRequestDTO("Tarefa", "Descrição", Priority.HIGH, LocalDate.now().plusDays(1), assigneeId);
    }


    @Test
    void shouldCreateTaskWithoutAssignee() {
        TaskRequestDTO dto = request(null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskMapper.toEntity(dto, project)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponseDTO result = taskService.create(projectId, dto);

        assertThat(result).isEqualTo(response);
        assertThat(task.getAssignee()).isNull();
        verifyNoInteractions(userRepository, projectMemberRepository);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new AuditEvent(AuditEntityType.TASK, taskId, AuditAction.CREATE, null, response));
    }

    @Test
    void shouldCreateTaskWithAssigneeWhoIsProjectMember() {
        TaskRequestDTO dto = request(userId);
        User user = new User("Ana", "ana@email.com");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskMapper.toEntity(dto, project)).thenReturn(task);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        taskService.create(projectId, dto);

        assertThat(task.getAssignee()).isSameAs(user);
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnCreateTask() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(projectId, request(null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(projectId.toString());
        verify(taskRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturn404WhenAssigneeNotFound() {
        TaskRequestDTO dto = request(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskMapper.toEntity(dto, project)).thenReturn(task);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(projectId, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
        verify(taskRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturn409WhenAssigneeIsNotProjectMember() {
        TaskRequestDTO dto = request(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskMapper.toEntity(dto, project)).thenReturn(task);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User("Ana", "ana@email.com")));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(projectId, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("O responsável precisa ser membro do projeto");
        verify(taskRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }


    @Test
    void shouldUpdateTaskStatus() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        TaskResponseDTO after = new TaskResponseDTO(taskId, "Tarefa", "Descrição", Status.IN_PROGRESS, Priority.HIGH,
                response.deadline(), projectId, null, null);
        when(taskMapper.toResponse(task)).thenReturn(response, after);

        taskService.updateStatus(taskId, Status.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new AuditEvent(AuditEntityType.TASK, taskId, AuditAction.UPDATE, response, after));
    }

    @Test
    void shouldReturn409WhenDoneTaskMovesBackToTodo() {
        task.changeStatus(Status.DONE);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, Status.TODO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Uma tarefa concluída não pode voltar para TODO");
        assertThat(task.getStatus()).isEqualTo(Status.DONE);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldAllowDoneTaskToMoveBackToInProgress() {
        task.changeStatus(Status.DONE);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        taskService.updateStatus(taskId, Status.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnUpdateStatus() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(taskId, Status.DONE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(taskId.toString());
        verify(eventPublisher, never()).publishEvent(any());
    }


    @Test
    void shouldListProjectTasksWithCombinedFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));
        when(taskMapper.toResponse(task)).thenReturn(response);

        Page<TaskResponseDTO> result = taskService.listByProject(projectId, Status.TODO, Priority.HIGH, userId, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldListProjectTasksWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<TaskResponseDTO> result = taskService.listByProject(projectId, null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnListTasks() {
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.listByProject(projectId, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(projectId.toString());
        verify(taskRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
