package br.com.taskhub.controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.dto.TaskStatusUpdateDTO;
import br.com.taskhub.entity.Priority;
import br.com.taskhub.entity.Status;
import br.com.taskhub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponseDTO> create(@PathVariable UUID projectId,
            @RequestBody @Valid TaskRequestDTO dto) {
        TaskResponseDTO response = taskService.create(projectId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<PagedModel<TaskResponseDTO>> findAll(@PathVariable UUID projectId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID assigneeId,
            @ParameterObject @PageableDefault(size = 20, sort = "deadline") Pageable pageable) {
        Page<TaskResponseDTO> page = taskService.listByProject(projectId, status, priority, assigneeId, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid TaskStatusUpdateDTO dto) {
        TaskResponseDTO response = taskService.updateStatus(id, dto.status());
        return ResponseEntity.ok(response);
    }
}
