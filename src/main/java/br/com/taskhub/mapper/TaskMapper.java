package br.com.taskhub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.entity.Project;
import br.com.taskhub.entity.Task;

@Mapper (componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskMapper {

    @Mapping (target = "id", ignore = true)
    @Mapping (target = "description", source = "dto.description")
    Task toEntity(TaskRequestDTO dto, Project project);

    @Mapping (target = "projectId", source = "project.id")
    TaskResponseDTO toResponse(Task task);
}
