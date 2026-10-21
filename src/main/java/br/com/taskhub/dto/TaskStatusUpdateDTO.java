package br.com.taskhub.dto;

import br.com.taskhub.entity.Status;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateDTO(

        @NotNull Status status

) {

}
