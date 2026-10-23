package br.com.taskhub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.taskhub.dto.ProjectRequestDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.AuditLog;
import br.com.taskhub.repository.AuditLogRepository;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith (MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ActorProvider actorProvider;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AuditEventListener listener;
    private UUID entityId;

    @BeforeEach
    void setUp() {
        listener = new AuditEventListener(auditLogRepository, actorProvider, JsonMapper.builder().build());
        entityId = UUID.randomUUID();
        when(actorProvider.currentActor()).thenReturn("ana");
    }

    @Test
    void shouldSaveCreateEventWithNewValueAsJson() {
        listener.onAuditEvent(AuditEvent.created(AuditEntityType.PROJECT, entityId, new ProjectRequestDTO("Projeto", "Descrição")));

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.PROJECT);
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getActor()).isEqualTo("ana");
        assertThat(saved.getOldValue()).isNull();
        assertThat(saved.getNewValue()).isEqualTo("{\"name\":\"Projeto\",\"description\":\"Descrição\"}");
    }

    @Test
    void shouldSaveUpdateEventWithOldAndNewValues() {
        listener.onAuditEvent(AuditEvent.updated(AuditEntityType.PROJECT, entityId,
                new ProjectRequestDTO("Antigo", null), new ProjectRequestDTO("Novo", null)));

        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(auditLogCaptor.getValue().getOldValue()).contains("\"name\":\"Antigo\"");
        assertThat(auditLogCaptor.getValue().getNewValue()).contains("\"name\":\"Novo\"");
    }

    @Test
    void shouldSaveDeleteEventWithoutNewValue() {
        listener.onAuditEvent(AuditEvent.deleted(AuditEntityType.PROJECT, entityId, new ProjectRequestDTO("Projeto", null)));

        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(auditLogCaptor.getValue().getOldValue()).contains("\"name\":\"Projeto\"");
        assertThat(auditLogCaptor.getValue().getNewValue()).isNull();
    }
}
