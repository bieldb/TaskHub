package br.com.taskhub.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.entity.Project;
import br.com.taskhub.entity.Status;
import br.com.taskhub.entity.Task;
import br.com.taskhub.exception.InvalidStatusTransitionException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.TaskMapper;
import br.com.taskhub.repository.ProjectRepository;
import br.com.taskhub.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectRepository projectRepository;

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
        Task saved = taskRepository.save(task);
        return taskMapper.toResponse(saved);
    }

    @Transactional (readOnly = true)
    public List<TaskResponseDTO> listByProject(UUID projectId) {
        if(!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Projeto", projectId);
        }
        return taskRepository.findByProjectId(projectId).stream().map(taskMapper::toResponse).toList();
    }
}
