package br.com.taskhub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.com.taskhub.dto.UserResponseDTO;
import br.com.taskhub.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    UserResponseDTO toResponse(User user);
}
