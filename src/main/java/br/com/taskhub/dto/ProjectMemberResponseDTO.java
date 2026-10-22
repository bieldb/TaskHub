package br.com.taskhub.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.taskhub.entity.ProjectRole;

public record ProjectMemberResponseDTO(

        UUID id,
        UUID projectId,
        UUID userId,
        String userName,
        ProjectRole role,
        LocalDateTime joinedAt

) {

}
