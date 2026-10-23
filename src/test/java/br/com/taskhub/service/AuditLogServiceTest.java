package br.com.taskhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.taskhub.dto.AuditLogResponseDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.AuditLog;
import br.com.taskhub.mapper.AuditLogMapper;
import br.com.taskhub.repository.AuditLogRepository;

@ExtendWith (MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private UUID entityId;
    private Pageable pageable;
    private AuditLog auditLog;
    private AuditLogResponseDTO response;

    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
        auditLog = new AuditLog(AuditEntityType.PROJECT, entityId, AuditAction.CREATE, "ana", null, "{\"name\":\"Projeto\"}");
        response = new AuditLogResponseDTO(UUID.randomUUID(), AuditEntityType.PROJECT, entityId, AuditAction.CREATE,
                "ana", null, "{\"name\":\"Projeto\"}", LocalDateTime.now());
    }

    @Test
    void shouldListAuditLogsWithCombinedFilters() {
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(response);

        Page<AuditLogResponseDTO> result = auditLogService.list(AuditEntityType.PROJECT, entityId, AuditAction.CREATE, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldListAuditLogsWithoutFilters() {
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AuditLogResponseDTO> result = auditLogService.list(null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
