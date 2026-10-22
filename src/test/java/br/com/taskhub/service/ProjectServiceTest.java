package br.com.taskhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.taskhub.dto.ProjectRequestDTO;
import br.com.taskhub.dto.ProjectResponseDTO;
import br.com.taskhub.entity.Project;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.ProjectMapper;
import br.com.taskhub.repository.ProjectRepository;

@ExtendWith (MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    private UUID projectId;
    private Project project;
    private ProjectRequestDTO request;
    private ProjectResponseDTO response;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new Project("Projeto", "Descrição");
        request = new ProjectRequestDTO("Projeto", "Descrição");
        response = new ProjectResponseDTO(projectId, "Projeto", "Descrição", LocalDateTime.now());
    }

    @Test
    void shouldCreateProject() {
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponseDTO result = projectService.create(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldListProjectsPaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(project), pageable, 1));
        when(projectMapper.toResponse(project)).thenReturn(response);

        Page<ProjectResponseDTO> result = projectService.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldFindProjectById() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponseDTO result = projectService.findById(projectId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldReturn404WhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(projectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(projectId.toString());
    }

    @Test
    void shouldUpdateProjectWithoutCallingSave() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponseDTO result = projectService.update(projectId, request);

        assertThat(result).isEqualTo(response);
        verify(projectMapper).updateEntity(request, project);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnUpdate() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update(projectId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(projectMapper, never()).updateEntity(any(), any());
    }

    @Test
    void shouldDeleteProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        projectService.delete(projectId);

        verify(projectRepository).delete(project);
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnDelete() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(projectId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(projectRepository, never()).delete(any());
    }
}
