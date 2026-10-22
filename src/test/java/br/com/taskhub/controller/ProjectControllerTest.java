package br.com.taskhub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.taskhub.dto.ProjectRequestDTO;
import br.com.taskhub.dto.ProjectResponseDTO;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.service.ProjectService;

@WebMvcTest (ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    private UUID projectId;
    private ProjectResponseDTO response;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        response = new ProjectResponseDTO(projectId, "Projeto", "Descrição", LocalDateTime.now());
    }

    @Test
    void shouldReturn201WithLocationWhenProjectIsCreated() throws Exception {
        when(projectService.create(any(ProjectRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Projeto", "description": "Descrição"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/projects/" + projectId))
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Projeto"));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenNameIsInvalid() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "ab"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")));

        verifyNoInteractions(projectService);
    }

    @Test
    void shouldReturn400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou malformado"));

        verifyNoInteractions(projectService);
    }

    @Test
    void shouldReturnPagedProjectsUsingDefaultPageable() throws Exception {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name"));
        when(projectService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectService).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void shouldReturnProjectById() throws Exception {
        when(projectService.findById(projectId)).thenReturn(response);

        mockMvc.perform(get("/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.description").value("Descrição"));
    }

    @Test
    void shouldReturn404WhenProjectNotFound() throws Exception {
        when(projectService.findById(projectId)).thenThrow(new ResourceNotFoundException("Projeto", projectId));

        mockMvc.perform(get("/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Projeto não encontrado(a): " + projectId));
    }

    @Test
    void shouldReturn400WhenIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/projects/{id}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectService);
    }

    @Test
    void shouldReturn200WhenProjectIsUpdated() throws Exception {
        when(projectService.update(any(UUID.class), any(ProjectRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/projects/{id}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Projeto", "description": "Descrição"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projeto"));

        verify(projectService).update(projectId, new ProjectRequestDTO("Projeto", "Descrição"));
    }

    @Test
    void shouldReturn204WhenProjectIsDeleted() throws Exception {
        mockMvc.perform(delete("/projects/{id}", projectId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(projectService).delete(projectId);
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentProject() throws Exception {
        doThrow(new ResourceNotFoundException("Projeto", projectId)).when(projectService).delete(projectId);

        mockMvc.perform(delete("/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
