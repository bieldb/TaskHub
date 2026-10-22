package br.com.taskhub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;


@SpringBootTest
@AutoConfigureMockMvc
@Import (TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE tasks, project_members, projects, users");
    }

    protected UUID createProject(String name) throws Exception {
        String body = """
                {"name": "%s", "description": "Descrição"}
                """.formatted(name);
        return postAndReturnId("/projects", body);
    }

    protected UUID createUser(String name, String email) throws Exception {
        String body = """
                {"name": "%s", "email": "%s"}
                """.formatted(name, email);
        return postAndReturnId("/users", body);
    }

    protected void addMember(UUID projectId, UUID userId) throws Exception {
        String body = """
                {"userId": "%s", "role": "MEMBER"}
                """.formatted(userId);
        postAndReturnId("/projects/" + projectId + "/members", body);
    }

    protected UUID createTask(UUID projectId, String title, String priority, UUID assigneeId) throws Exception {
        String assignee = assigneeId == null ? "null" : "\"" + assigneeId + "\"";
        String body = """
                {"title": "%s", "priority": "%s", "assigneeId": %s}
                """.formatted(title, priority, assignee);
        return postAndReturnId("/projects/" + projectId + "/tasks", body);
    }

    protected void updateStatus(UUID taskId, String newStatus) throws Exception {
        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "%s"}
                        """.formatted(newStatus)))
                .andExpect(status().isOk());
    }

    private UUID postAndReturnId(String uri, String body) throws Exception {
        String response = mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }
}
