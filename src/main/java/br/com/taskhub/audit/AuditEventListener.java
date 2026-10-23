package br.com.taskhub.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.taskhub.entity.AuditLog;
import br.com.taskhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final ActorProvider actorProvider;
    private final JsonMapper jsonMapper;

    @TransactionalEventListener (phase = TransactionPhase.BEFORE_COMMIT)
    public void onAuditEvent(AuditEvent event) {
        AuditLog auditLog = new AuditLog(
                event.entityType(),
                event.entityId(),
                event.action(),
                actorProvider.currentActor(),
                toJson(event.oldValue()),
                toJson(event.newValue()));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        return value == null ? null : jsonMapper.writeValueAsString(value);
    }
}
