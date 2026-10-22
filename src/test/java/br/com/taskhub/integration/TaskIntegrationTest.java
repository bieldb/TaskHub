package br.com.taskhub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.taskhub.AbstractIntegrationTest;

class TaskIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldRunTaskLifecycleAndBlockDoneTaskFromMovingBackToTodo() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID userId = createUser("Ana", "ana@email.com");
        addMember(projectId, userId);

        UUID taskId = createTask(projectId, "Tarefa", "HIGH", userId);
        updateStatus(taskId, "IN_PROGRESS");
        updateStatus(taskId, "DONE");

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "TODO"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Uma tarefa concluída não pode voltar para TODO"));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("DONE"))
                .andExpect(jsonPath("$.content[0].assigneeId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].assigneeName").value("Ana"));
    }

    @Test
    void shouldFilterTasksByCombinedFiltersWithPagination() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID ana = createUser("Ana", "ana@email.com");
        UUID bia = createUser("Bia", "bia@email.com");
        addMember(projectId, ana);
        addMember(projectId, bia);

        createTask(projectId, "Tarefa 1", "HIGH", ana);
        UUID inProgress = createTask(projectId, "Tarefa 2", "HIGH", ana);
        createTask(projectId, "Tarefa 3", "LOW", ana);
        createTask(projectId, "Tarefa 4", "HIGH", bia);
        createTask(projectId, "Tarefa 5", "HIGH", ana);
        createTask(projectId, "Tarefa 6", "HIGH", null);
        updateStatus(inProgress, "IN_PROGRESS");

        // Outro projeto com tarefa que também atende aos filtros: não pode aparecer na listagem.
        UUID otherProjectId = createProject("Outro projeto");
        addMember(otherProjectId, ana);
        createTask(otherProjectId, "Tarefa de outro projeto", "HIGH", ana);

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId)
                .param("status", "TODO")
                .param("priority", "HIGH")
                .param("assigneeId", ana.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title").value(containsInAnyOrder("Tarefa 1", "Tarefa 5")))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId)
                .param("status", "TODO")
                .param("priority", "HIGH")
                .param("assigneeId", ana.toString())
                .param("page", "1")
                .param("size", "1")
                .param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Tarefa 1"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId).param("priority", "HIGH"))
                .andExpect(jsonPath("$.page.totalElements").value(5));
    }

    @Test
    void shouldReturn409AndNotPersistTaskWhenAssigneeIsNotProjectMember() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID outsider = createUser("Ana", "ana@email.com");

        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "HIGH", "assigneeId": "%s"}
                        """.formatted(outsider)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O responsável precisa ser membro do projeto"));

        assertThat(countTasks()).isZero();
    }

    @Test
    void shouldReturn404WhenAssigneeDoesNotExist() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID nonexistentUser = UUID.randomUUID();

        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Tarefa", "priority": "HIGH", "assigneeId": "%s"}
                        """.formatted(nonexistentUser)))
                .andExpect(status().isNotFound());

        assertThat(countTasks()).isZero();
    }

    @Test
    void shouldReturn400WhenSortPropertyDoesNotExist() throws Exception {
        UUID projectId = createProject("Projeto");

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId).param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro de ordenação inválido"));
    }

    @Test
    void shouldDeleteTasksAndMembersWhenProjectIsDeleted() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID userId = createUser("Ana", "ana@email.com");
        addMember(projectId, userId);
        createTask(projectId, "Tarefa", "HIGH", userId);

        mockMvc.perform(delete("/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId))
                .andExpect(status().isNotFound());
        assertThat(countTasks()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_members", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    private int countTasks() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tasks", Integer.class);
    }
}
