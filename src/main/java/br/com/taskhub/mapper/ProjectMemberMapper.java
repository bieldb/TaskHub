package br.com.taskhub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import br.com.taskhub.dto.ProjectMemberResponseDTO;
import br.com.taskhub.entity.ProjectMember;

@Mapper (componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProjectMemberMapper {

    @Mapping (target = "projectId", source = "project.id")
    @Mapping (target = "userId", source = "user.id")
    @Mapping (target = "userName", source = "user.name")
    ProjectMemberResponseDTO toResponse(ProjectMember projectMember);
}
