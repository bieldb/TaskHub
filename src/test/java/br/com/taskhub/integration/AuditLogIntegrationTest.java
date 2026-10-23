package br.com.taskhub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import br.com.taskhub.AbstractIntegrationTest;

class AuditLogIntegrationTest extends AbstractIntegrationTest {

    private static final String ACTOR_HEADER = "X-User-Id";

    @Test
    void shouldRecordCreateUpdateAndDeleteOfProjectWithActor() throws Exception {
        String created = mockMvc.perform(post("/projects")
                .header(ACTOR_HEADER, "ana")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Projeto", "description": "Descrição"}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(put("/projects/{id}", projectId)
                .header(ACTOR_HEADER, "ana")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Projeto renomeado", "description": "Descrição"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/projects/{id}", projectId).header(ACTOR_HEADER, "bia"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/audit-logs")
                .param("entityType", "PROJECT")
                .param("entityId", projectId.toString())
                .param("sort", "occurredAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[0].actor").value("ana"))
                .andExpect(jsonPath("$.content[0].oldValue").isEmpty())
                .andExpect(jsonPath("$.content[0].newValue.name").value("Projeto"))
                .andExpect(jsonPath("$.content[0].newValue.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.content[1].action").value("UPDATE"))
                .andExpect(jsonPath("$.content[1].oldValue.name").value("Projeto"))
                .andExpect(jsonPath("$.content[1].newValue.name").value("Projeto renomeado"))
                .andExpect(jsonPath("$.content[2].action").value("DELETE"))
                .andExpect(jsonPath("$.content[2].actor").value("bia"))
                .andExpect(jsonPath("$.content[2].oldValue.name").value("Projeto renomeado"))
                .andExpect(jsonPath("$.content[2].newValue").isEmpty());
    }

    @Test
    void shouldRecordOldAndNewStatusOnTaskStatusUpdate() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID taskId = createTask(projectId, "Tarefa", "HIGH", null);

        updateStatus(taskId, "IN_PROGRESS");

        mockMvc.perform(get("/audit-logs")
                .param("entityType", "TASK")
                .param("entityId", taskId.toString())
                .param("action", "UPDATE"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].oldValue.status").value("TODO"))
                .andExpect(jsonPath("$.content[0].newValue.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldRecordAnonymousActorWhenHeaderIsMissing() throws Exception {
        UUID userId = createUser("Ana", "ana@email.com");

        mockMvc.perform(get("/audit-logs").param("entityId", userId.toString()))
                .andExpect(jsonPath("$.content[0].entityType").value("USER"))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[0].actor").value("anonymous"))
                .andExpect(jsonPath("$.content[0].newValue.email").value("ana@email.com"));
    }

    @Test
    void shouldRecordMemberAddition() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID userId = createUser("Ana", "ana@email.com");
        addMember(projectId, userId);

        mockMvc.perform(get("/audit-logs").param("entityType", "PROJECT_MEMBER"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].newValue.userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].newValue.role").value("MEMBER"));
    }

    @Test
    void shouldNotRecordAnythingWhenOperationFails() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID taskId = createTask(projectId, "Tarefa", "HIGH", null);
        updateStatus(taskId, "DONE");
        createUser("Ana", "ana@email.com");
        int before = countAuditLogs();

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "TODO"}
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Outra Ana", "email": "ANA@email.com"}
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/projects/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Projeto"}
                        """))
                .andExpect(status().isNotFound());

        assertThat(countAuditLogs()).isEqualTo(before);
    }

    @Test
    void shouldKeepAuditLogAfterProjectIsDeleted() throws Exception {
        UUID projectId = createProject("Projeto");

        mockMvc.perform(delete("/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_id = ?", Integer.class, projectId)).isEqualTo(2);
    }

    private int countAuditLogs() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);
    }
}
