package br.com.taskhub.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.taskhub.AbstractIntegrationTest;

class UserAndMemberIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldNormalizeEmailAndRejectDuplicateIgnoringCase() throws Exception {
        String location = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": " Ana ", "email": "Ana@Email.COM"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@email.com"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Outra Ana", "email": "ANA@email.com"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com o email ana@email.com"));
    }

    @Test
    void shouldAddAndListProjectMembers() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID userId = createUser("Ana", "ana@email.com");

        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "OWNER"}
                        """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty());

        mockMvc.perform(get("/projects/{projectId}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].userName").value("Ana"));
    }

    @Test
    void shouldReturn409WhenUserIsAlreadyMember() throws Exception {
        UUID projectId = createProject("Projeto");
        UUID userId = createUser("Ana", "ana@email.com");
        addMember(projectId, userId);

        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "VIEWER"}
                        """.formatted(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Usuário já é membro do projeto"));
    }

    @Test
    void shouldReturn404WhenAddingNonexistentUserAsMember() throws Exception {
        UUID projectId = createProject("Projeto");

        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "MEMBER"}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201WithLocationThatResolvesToCreatedProject() throws Exception {
        String location = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Projeto", "description": "Descrição"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projeto"));
    }
}
