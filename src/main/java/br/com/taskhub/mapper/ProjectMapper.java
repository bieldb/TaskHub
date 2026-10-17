package br.com.taskhub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import br.com.taskhub.dto.ProjectRequestDTO;
import br.com.taskhub.dto.ProjectResponseDTO;
import br.com.taskhub.entity.Project;

@Mapper (componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProjectMapper {

    @Mapping (target = "id", ignore = true)
    Project toEntity(ProjectRequestDTO dto);

    @Mapping (target = "id", ignore = true)
    void updateEntity(ProjectRequestDTO dto, @MappingTarget Project project);

    ProjectResponseDTO toResponse(Project project);
}
