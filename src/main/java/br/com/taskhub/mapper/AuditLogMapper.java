package br.com.taskhub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.com.taskhub.dto.AuditLogResponseDTO;
import br.com.taskhub.entity.AuditLog;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditLogMapper {

    AuditLogResponseDTO toResponse(AuditLog auditLog);
}
