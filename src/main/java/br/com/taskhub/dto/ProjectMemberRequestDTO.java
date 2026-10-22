package br.com.taskhub.dto;

import java.util.UUID;

import br.com.taskhub.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequestDTO(

        @NotNull(message = "O usuário é obrigatório") UUID userId,
        @NotNull(message = "A função é obrigatória") ProjectRole role

) {

}
