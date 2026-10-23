package br.com.taskhub.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.taskhub.audit.AuditEvent;
import br.com.taskhub.dto.ProjectMemberRequestDTO;
import br.com.taskhub.dto.ProjectMemberResponseDTO;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.Project;
import br.com.taskhub.entity.ProjectMember;
import br.com.taskhub.entity.User;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.ProjectMemberMapper;
import br.com.taskhub.repository.ProjectMemberRepository;
import br.com.taskhub.repository.ProjectRepository;
import br.com.taskhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProjectMemberResponseDTO addMember(UUID projectId, ProjectMemberRequestDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projectId));
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.userId()));
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, dto.userId())) {
            throw new ConflictException("Usuário já é membro do projeto");
        }
        ProjectMember projectMember = new ProjectMember(project, user, dto.role());
        ProjectMember saved = projectMemberRepository.save(projectMember);
        ProjectMemberResponseDTO response = projectMemberMapper.toResponse(saved);
        eventPublisher.publishEvent(AuditEvent.created(AuditEntityType.PROJECT_MEMBER, saved.getId(), response));
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponseDTO> listMembers(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Projeto", projectId);
        }
        return projectMemberRepository.findByProjectId(projectId).stream().map(projectMemberMapper::toResponse)
                .toList();
    }
}
