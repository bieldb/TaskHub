package br.com.taskhub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.entity.Priority;
import br.com.taskhub.entity.Project;
import br.com.taskhub.entity.Status;
import br.com.taskhub.entity.Task;
import br.com.taskhub.entity.User;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.InvalidStatusTransitionException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.TaskMapper;
import br.com.taskhub.repository.ProjectMemberRepository;
import br.com.taskhub.repository.ProjectRepository;
import br.com.taskhub.repository.TaskRepository;
import br.com.taskhub.repository.TaskSpecifications;
import br.com.taskhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional 
    public TaskResponseDTO updateStatus(UUID taskId, Status newStatus) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Tarefa", taskId));

        if(task.getStatus() == Status.DONE && newStatus == Status.TODO) {
            throw new InvalidStatusTransitionException("Uma tarefa concluída não pode voltar para TODO");
        }
        task.changeStatus(newStatus);
        return taskMapper.toResponse(task);
    }

    @Transactional 
    public TaskResponseDTO create(UUID projectId, TaskRequestDTO dto) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Projeto", projectId));

        Task task = taskMapper.toEntity(dto, project);
        task.setAssignee(resolveAssignee(projectId, dto.assigneeId()));
        Task saved = taskRepository.save(task);
        return taskMapper.toResponse(saved);
    }

    @Transactional (readOnly = true)
    public Page<TaskResponseDTO> listByProject(UUID projectId, Status status, Priority priority, UUID assigneeId, Pageable pageable) {
        if(!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Projeto", projectId);
        }
        Specification<Task> spec = TaskSpecifications.belongsToProject(projectId);
        if(status != null) {
            spec = spec.and(TaskSpecifications.hasStatus(status));
        }
        if(priority != null) {
            spec = spec.and(TaskSpecifications.hasPriority(priority));
        }
        if(assigneeId != null) {
            spec = spec.and(TaskSpecifications.hasAssignee(assigneeId));
        }
        return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
    }

    private User resolveAssignee(UUID projectId, UUID assigneeId) {
        if(assigneeId == null) {
            return null;
        }
        User assignee = userRepository.findById(assigneeId).orElseThrow(() -> new ResourceNotFoundException("Usuário", assigneeId));
        if(!projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)) {
            throw new ConflictException("O responsável precisa ser membro do projeto");
        }
        return assignee;
    }
}
