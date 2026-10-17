package br.com.taskhub.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponseDTO(UUID id, String name, String description, LocalDateTime createdAt) {

}
