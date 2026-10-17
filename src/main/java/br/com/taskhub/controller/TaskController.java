package br.com.taskhub.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.taskhub.dto.TaskRequestDTO;
import br.com.taskhub.dto.TaskResponseDTO;
import br.com.taskhub.dto.TaskStatusUpdateDTO;
import br.com.taskhub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
public class TaskController {

    private final TaskService taskService;

    @PostMapping ("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponseDTO> create(@PathVariable UUID projectId, @RequestBody @Valid TaskRequestDTO dto) {
        TaskResponseDTO response = taskService.create(projectId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping ("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> findAll(@PathVariable UUID projectId) {
        List<TaskResponseDTO> tasks = taskService.listByProject(projectId);
        return ResponseEntity.ok(tasks);

    }

    @PatchMapping ("/tasks/{id}/status")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid TaskStatusUpdateDTO dto) {
        TaskResponseDTO response = taskService.updateStatus(id, dto.status());
        return ResponseEntity.ok(response);
    }
}
