package br.com.taskhub.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;

import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;

public record AuditLogResponseDTO(

        UUID id,
        AuditEntityType entityType,
        UUID entityId,
        AuditAction action,
        String actor,
        @JsonRawValue String oldValue,
        @JsonRawValue String newValue,
        LocalDateTime occurredAt

) {

}
