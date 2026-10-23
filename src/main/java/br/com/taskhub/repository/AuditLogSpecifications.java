package br.com.taskhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.AuditLog;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {

    }
    public static Specification<AuditLog> hasEntityType(AuditEntityType entityType) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }
    public static Specification<AuditLog> hasEntityId(UUID entityId) {
        return (root, query, cb) -> cb.equal(root.get("entityId"), entityId);
    }
    public static Specification<AuditLog> hasAction(AuditAction action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }
}
