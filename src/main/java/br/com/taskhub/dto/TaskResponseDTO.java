package br.com.taskhub.dto;

import java.time.LocalDate;
import java.util.UUID;

import br.com.taskhub.entity.Priority;
import br.com.taskhub.entity.Status;

public record TaskResponseDTO(

                UUID id,
                String title,
                String description,
                Status status,
                Priority priority,
                LocalDate deadline,
                UUID projectId

) {

}
