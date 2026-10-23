package br.com.taskhub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.taskhub.dto.AuditLogResponseDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.AuditLog;
import br.com.taskhub.mapper.AuditLogMapper;
import br.com.taskhub.repository.AuditLogRepository;
import br.com.taskhub.repository.AuditLogSpecifications;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional (readOnly = true)
    public Page<AuditLogResponseDTO> list(AuditEntityType entityType, UUID entityId, AuditAction action, Pageable pageable) {
        Specification<AuditLog> spec = Specification.unrestricted();
        if (entityType != null) {
            spec = spec.and(AuditLogSpecifications.hasEntityType(entityType));
        }
        if (entityId != null) {
            spec = spec.and(AuditLogSpecifications.hasEntityId(entityId));
        }
        if (action != null) {
            spec = spec.and(AuditLogSpecifications.hasAction(action));
        }
        return auditLogRepository.findAll(spec, pageable).map(auditLogMapper::toResponse);
    }
}
