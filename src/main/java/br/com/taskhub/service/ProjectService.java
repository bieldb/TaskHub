package br.com.taskhub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.taskhub.audit.AuditEvent;
import br.com.taskhub.dto.ProjectRequestDTO;
import br.com.taskhub.dto.ProjectResponseDTO;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.Project;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.ProjectMapper;
import br.com.taskhub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional 
    public ProjectResponseDTO create(ProjectRequestDTO dto) {
        Project project = projectMapper.toEntity(dto);
        Project saved = projectRepository.save(project);
        ProjectResponseDTO response = projectMapper.toResponse(saved);
        eventPublisher.publishEvent(AuditEvent.created(AuditEntityType.PROJECT, saved.getId(), response));
        return response;
    }


    @Transactional (readOnly = true)
    public Page<ProjectResponseDTO> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable).map(projectMapper::toResponse);
    }

    @Transactional (readOnly = true)
    public ProjectResponseDTO findById(UUID id) {
        return projectMapper.toResponse(findProjectOrThrow(id));
    }

    @Transactional 
    public ProjectResponseDTO update(UUID id, ProjectRequestDTO dto) {
        Project project = findProjectOrThrow(id);
        ProjectResponseDTO before = projectMapper.toResponse(project);
        projectMapper.updateEntity(dto, project);
        ProjectResponseDTO after = projectMapper.toResponse(project);
        eventPublisher.publishEvent(AuditEvent.updated(AuditEntityType.PROJECT, id, before, after));
        return after;
    }

    @Transactional 
    public void delete(UUID id) {
        Project project = findProjectOrThrow(id);
        ProjectResponseDTO before = projectMapper.toResponse(project);
        projectRepository.delete(project);
        eventPublisher.publishEvent(AuditEvent.deleted(AuditEntityType.PROJECT, id, before));
    }


    private Project findProjectOrThrow(UUID id) {
        return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
    }
}
