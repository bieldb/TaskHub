package br.com.taskhub.audit;

import java.util.UUID;

import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;

public record AuditEvent(

        AuditEntityType entityType,
        UUID entityId,
        AuditAction action,
        Object oldValue,
        Object newValue

) {

    public static AuditEvent created(AuditEntityType entityType, UUID entityId, Object newValue) {
        return new AuditEvent(entityType, entityId, AuditAction.CREATE, null, newValue);
    }

    public static AuditEvent updated(AuditEntityType entityType, UUID entityId, Object oldValue, Object newValue) {
        return new AuditEvent(entityType, entityId, AuditAction.UPDATE, oldValue, newValue);
    }

    public static AuditEvent deleted(AuditEntityType entityType, UUID entityId, Object oldValue) {
        return new AuditEvent(entityType, entityId, AuditAction.DELETE, oldValue, null);
    }
}
