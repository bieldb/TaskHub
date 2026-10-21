package br.com.taskhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequestDTO(

                @NotBlank @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres") String name,
                @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres") String description

) {

}
