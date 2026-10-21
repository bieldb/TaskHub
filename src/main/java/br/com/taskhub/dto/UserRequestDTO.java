package br.com.taskhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(

        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email) {

}
