package br.com.taskhub.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.taskhub.dto.ProjectMemberRequestDTO;
import br.com.taskhub.dto.ProjectMemberResponseDTO;
import br.com.taskhub.entity.ProjectRole;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.service.ProjectMemberService;

@WebMvcTest (ProjectMemberController.class)
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectMemberService projectMemberService;

    private UUID projectId;
    private UUID userId;
    private ProjectMemberResponseDTO response;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        response = new ProjectMemberResponseDTO(UUID.randomUUID(), projectId, userId, "Ana", ProjectRole.MEMBER,
                LocalDateTime.now());
    }

    @Test
    void shouldReturn201WhenMemberIsAdded() throws Exception {
        when(projectMemberService.addMember(eq(projectId), any(ProjectMemberRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "MEMBER"}
                        """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.userName").value("Ana"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        verify(projectMemberService).addMember(projectId, new ProjectMemberRequestDTO(userId, ProjectRole.MEMBER));
    }

    @Test
    void shouldReturn400WhenUserIdAndRoleAreMissing() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("userId", "role")))
                .andExpect(jsonPath("$.errors[*].message")
                        .value(containsInAnyOrder("O usuário é obrigatório", "A função é obrigatória")));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void shouldReturn400WhenRoleIsNotAValidEnum() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "ADMIN"}
                        """.formatted(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnAddMember() throws Exception {
        when(projectMemberService.addMember(eq(projectId), any(ProjectMemberRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Projeto", projectId));

        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "MEMBER"}
                        """.formatted(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void shouldReturn409WhenUserIsAlreadyMember() throws Exception {
        when(projectMemberService.addMember(eq(projectId), any(ProjectMemberRequestDTO.class)))
                .thenThrow(new ConflictException("Usuário já é membro do projeto"));

        mockMvc.perform(post("/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "MEMBER"}
                        """.formatted(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Usuário já é membro do projeto"));
    }

    @Test
    void shouldListProjectMembers() throws Exception {
        when(projectMemberService.listMembers(projectId)).thenReturn(List.of(response));

        mockMvc.perform(get("/projects/{projectId}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userName").value("Ana"));
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnListMembers() throws Exception {
        when(projectMemberService.listMembers(projectId)).thenThrow(new ResourceNotFoundException("Projeto", projectId));

        mockMvc.perform(get("/projects/{projectId}/members", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
