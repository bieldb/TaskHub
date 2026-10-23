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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import br.com.taskhub.audit.AuditEvent;
import br.com.taskhub.dto.ProjectMemberRequestDTO;
import br.com.taskhub.dto.ProjectMemberResponseDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.Project;
import br.com.taskhub.entity.ProjectMember;
import br.com.taskhub.entity.ProjectRole;
import br.com.taskhub.entity.User;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.ProjectMemberMapper;
import br.com.taskhub.repository.ProjectMemberRepository;
import br.com.taskhub.repository.ProjectRepository;
import br.com.taskhub.repository.UserRepository;

@ExtendWith (MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<ProjectMember> memberCaptor;

    private UUID projectId;
    private UUID userId;
    private Project project;
    private User user;
    private ProjectMemberRequestDTO request;
    private ProjectMemberResponseDTO response;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        project = new Project("Projeto", "Descrição");
        user = new User("Ana", "ana@email.com");
        request = new ProjectMemberRequestDTO(userId, ProjectRole.MEMBER);
        response = new ProjectMemberResponseDTO(UUID.randomUUID(), projectId, userId, "Ana", ProjectRole.MEMBER,
                LocalDateTime.now());
    }

    @Test
    void shouldAddMemberToProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectMemberMapper.toResponse(any(ProjectMember.class))).thenReturn(response);

        ProjectMemberResponseDTO result = projectMemberService.addMember(projectId, request);

        assertThat(result).isEqualTo(response);
        verify(projectMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getProject()).isSameAs(project);
        assertThat(memberCaptor.getValue().getUser()).isSameAs(user);
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(ProjectRole.MEMBER);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().entityType()).isEqualTo(AuditEntityType.PROJECT_MEMBER);
        assertThat(eventCaptor.getValue().action()).isEqualTo(AuditAction.CREATE);
        assertThat(eventCaptor.getValue().oldValue()).isNull();
        assertThat(eventCaptor.getValue().newValue()).isEqualTo(response);
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnAddMember() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(projectId.toString());
        verify(projectMemberRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturn404WhenUserNotFoundOnAddMember() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
        verify(projectMemberRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReturn409WhenUserIsAlreadyMember() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Usuário já é membro do projeto");
        verify(projectMemberRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldListProjectMembers() {
        ProjectMember member = new ProjectMember(project, user, ProjectRole.OWNER);
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(List.of(member));
        when(projectMemberMapper.toResponse(member)).thenReturn(response);

        List<ProjectMemberResponseDTO> result = projectMemberService.listMembers(projectId);

        assertThat(result).containsExactly(response);
    }

    @Test
    void shouldReturn404WhenProjectNotFoundOnListMembers() {
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.listMembers(projectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(projectId.toString());
        verify(projectMemberRepository, never()).findByProjectId(any());
    }
}
