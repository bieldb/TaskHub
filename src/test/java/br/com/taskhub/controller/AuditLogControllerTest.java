package br.com.taskhub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.taskhub.dto.AuditLogResponseDTO;
import br.com.taskhub.entity.AuditAction;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.service.AuditLogService;

@WebMvcTest (AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    private UUID entityId;
    private AuditLogResponseDTO response;

    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
        response = new AuditLogResponseDTO(UUID.randomUUID(), AuditEntityType.TASK, entityId, AuditAction.UPDATE, "ana",
                "{\"status\":\"TODO\"}", "{\"status\":\"DONE\"}", LocalDateTime.now());
    }

    @Test
    void shouldReturnAuditLogsWithValuesAsJsonObjects() throws Exception {
        when(auditLogService.list(eq(AuditEntityType.TASK), eq(entityId), eq(AuditAction.UPDATE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/audit-logs")
                .param("entityType", "TASK")
                .param("entityId", entityId.toString())
                .param("action", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("TASK"))
                .andExpect(jsonPath("$.content[0].entityId").value(entityId.toString()))
                .andExpect(jsonPath("$.content[0].action").value("UPDATE"))
                .andExpect(jsonPath("$.content[0].actor").value("ana"))
                .andExpect(jsonPath("$.content[0].oldValue.status").value("TODO"))
                .andExpect(jsonPath("$.content[0].newValue.status").value("DONE"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldReturnNullWhenValueIsAbsent() throws Exception {
        AuditLogResponseDTO created = new AuditLogResponseDTO(UUID.randomUUID(), AuditEntityType.PROJECT, entityId,
                AuditAction.CREATE, "anonymous", null, "{\"name\":\"Projeto\"}", LocalDateTime.now());
        when(auditLogService.list(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(created), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].oldValue").isEmpty())
                .andExpect(jsonPath("$.content[0].newValue.name").value("Projeto"));
    }

    @Test
    void shouldUseNewestFirstAsDefaultSort() throws Exception {
        when(auditLogService.list(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogService).list(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("occurredAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldReturn400WhenEntityTypeIsNotAValidEnum() throws Exception {
        mockMvc.perform(get("/audit-logs").param("entityType", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(auditLogService);
    }
}
