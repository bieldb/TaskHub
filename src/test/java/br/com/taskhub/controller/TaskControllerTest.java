package br.com.taskhub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.entity.Priority;
import br.com.taskhub.entity.Status;
import br.com.taskhub.entity.Task;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.InvalidStatusTransitionException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.service.TaskService;

@WebMvcTest (TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    private UUID projectId;
    private UUID taskId;
    private UUID userId;
    private TaskResponseDTO response;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();
        response = new TaskResponseDTO(taskId, "Tarefa", "Descrição", Status.TODO, Priority.HIGH,
                LocalDate.now().plusDays(1), projectId, userId, "Ana");
    }

    // POST /projects/{projectId}/tasks

    @Test
    void shouldReturn201WhenTaskIsCreated() throws Exception {
        when(taskService.create(eq(projectId), any(TaskRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "HIGH", "assigneeId": "%s"}
                        """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeId").value(userId.toString()))
                .andExpect(jsonPath("$.assigneeName").value("Ana"));

        verify(taskService).create(projectId, new TaskRequestDTO("Tarefa", null, Priority.HIGH, null, userId));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": " "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("title", "priority")));

        verifyNoInteractions(taskService);
    }

    @Test
    void shouldReturn400WhenDeadlineIsInThePast() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "HIGH", "deadline": "%s"}
                        """.formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("deadline")));

        verifyNoInteractions(taskService);
    }

    @Test
    void shouldReturn400WhenPriorityIsNotAValidEnum() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "URGENT"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        verifyNoInteractions(taskService);
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnCreateTask() throws Exception {
        when(taskService.create(eq(projectId), any(TaskRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Projeto", projectId));

        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "HIGH"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void shouldReturn409WhenAssigneeIsNotProjectMember() throws Exception {
        when(taskService.create(eq(projectId), any(TaskRequestDTO.class)))
                .thenThrow(new ConflictException("O responsável precisa ser membro do projeto"));

        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "HIGH", "assigneeId": "%s"}
                        """.formatted(userId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("O responsável precisa ser membro do projeto"));
    }

    // GET /projects/{projectId}/tasks

    @Test
    void shouldPassFiltersAndPageableToService() throws Exception {
        when(taskService.listByProject(eq(projectId), eq(Status.TODO), eq(Priority.HIGH), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId)
                .param("status", "TODO")
                .param("priority", "HIGH")
                .param("assigneeId", userId.toString())
                .param("page", "1")
                .param("size", "5")
                .param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).listByProject(eq(projectId), eq(Status.TODO), eq(Priority.HIGH), eq(userId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldUseDefaultPageableAndNullFiltersWhenNoParamsAreSent() throws Exception {
        when(taskService.listByProject(eq(projectId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).listByProject(eq(projectId), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("deadline").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldLimitPageSizeToMaxPageSize() throws Exception {
        when(taskService.listByProject(eq(projectId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId).param("size", "1000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).listByProject(eq(projectId), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void shouldReturn400WhenStatusFilterIsNotAValidEnum() throws Exception {
        mockMvc.perform(get("/projects/{projectId}/tasks", projectId).param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(taskService);
    }

    @Test
    void shouldReturn400WhenSortPropertyDoesNotExist() throws Exception {
        when(taskService.listByProject(eq(projectId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("foo", TypeInformation.of(Task.class), List.of()));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId).param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro de ordenação inválido"))
                .andExpect(jsonPath("$.detail").value("Não é possível ordenar por 'foo'"));
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnListTasks() throws Exception {
        when(taskService.listByProject(eq(projectId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Projeto", projectId));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // PATCH /tasks/{id}/status

    @Test
    void shouldReturn200WhenStatusIsUpdated() throws Exception {
        TaskResponseDTO updated = new TaskResponseDTO(taskId, "Tarefa", "Descrição", Status.IN_PROGRESS,
                Priority.HIGH, LocalDate.now().plusDays(1), projectId, userId, "Ana");
        when(taskService.updateStatus(taskId, Status.IN_PROGRESS)).thenReturn(updated);

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "IN_PROGRESS"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldReturn409WhenDoneTaskMovesBackToTodo() throws Exception {
        when(taskService.updateStatus(taskId, Status.TODO))
                .thenThrow(new InvalidStatusTransitionException("Uma tarefa concluída não pode voltar para TODO"));

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "TODO"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("Uma tarefa concluída não pode voltar para TODO"));
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnUpdateStatus() throws Exception {
        when(taskService.updateStatus(taskId, Status.DONE)).thenThrow(new ResourceNotFoundException("Tarefa", taskId));

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "DONE"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Tarefa não encontrado(a): " + taskId));
    }

    @Test
    void shouldReturn400WhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verifyNoInteractions(taskService);
    }

    @Test
    void shouldReturn400WhenStatusIsNotAValidEnum() throws Exception {
        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "FOO"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        verifyNoInteractions(taskService);
    }
}
