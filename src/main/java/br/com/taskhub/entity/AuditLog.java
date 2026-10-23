package br.com.taskhub.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table (name = "audit_log")
@Getter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated (EnumType.STRING)
    @Column (name = "entity_type", nullable = false, length = 30, updatable = false)
    private AuditEntityType entityType;

    @Column (name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    @Enumerated (EnumType.STRING)
    @Column (name = "action", nullable = false, length = 10, updatable = false)
    private AuditAction action;

    @Column (name = "actor", nullable = false, length = 100, updatable = false)
    private String actor;

    @JdbcTypeCode (SqlTypes.JSON)
    @Column (name = "old_value", updatable = false)
    private String oldValue;

    @JdbcTypeCode (SqlTypes.JSON)
    @Column (name = "new_value", updatable = false)
    private String newValue;

    @Column (name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    public AuditLog(AuditEntityType entityType, UUID entityId, AuditAction action, String actor, String oldValue,
            String newValue) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @PrePersist
    void onCreate() {
        this.occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
