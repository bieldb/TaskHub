package br.com.taskhub.dto;

import java.time.LocalDate;
import java.util.UUID;

import br.com.taskhub.entity.Priority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskRequestDTO(

                @NotBlank @Size(max = 150, message = "O título deve ter no máximo 150 caracteres") String title,
                @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres") String description,
                @NotNull Priority priority,
                @FutureOrPresent LocalDate deadline,
                UUID assigneeId

) {

}
