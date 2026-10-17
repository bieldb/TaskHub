package br.com.taskhub.controller;

import java.net.URI;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.taskhub.dto.ProjectRequestDTO;
import br.com.taskhub.dto.ProjectResponseDTO;
import br.com.taskhub.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/projects")
@RequiredArgsConstructor 
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping 
    public ResponseEntity<ProjectResponseDTO> create(@RequestBody @Valid ProjectRequestDTO dto) {
        ProjectResponseDTO response = projectService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping 
    public ResponseEntity<PagedModel<ProjectResponseDTO>> findAll(@ParameterObject @PageableDefault (size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(projectService.findAll(pageable)));
    }

    @GetMapping ("/{id}")
    public ResponseEntity<ProjectResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PutMapping ("/{id}")
    public ResponseEntity<ProjectResponseDTO> update(@PathVariable UUID id, @RequestBody @Valid ProjectRequestDTO dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
