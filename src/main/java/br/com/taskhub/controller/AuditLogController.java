package br.com.taskhub.controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.taskhub.dto.AuditLogResponseDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.service.AuditLogService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping ("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<PagedModel<AuditLogResponseDTO>> list(
            @RequestParam (required = false) AuditEntityType entityType,
            @RequestParam (required = false) UUID entityId,
            @RequestParam (required = false) AuditAction action,
            @ParameterObject @PageableDefault (size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(auditLogService.list(entityType, entityId, action, pageable)));
    }
}
