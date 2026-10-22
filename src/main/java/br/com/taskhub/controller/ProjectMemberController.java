package br.com.taskhub.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.taskhub.dto.ProjectMemberRequestDTO;
import br.com.taskhub.dto.ProjectMemberResponseDTO;
import br.com.taskhub.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/projects/{projectId}/members")
@RequiredArgsConstructor 
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping 
    public ResponseEntity<ProjectMemberResponseDTO> addMember(@PathVariable UUID projectId, @RequestBody @Valid ProjectMemberRequestDTO dto) {
        ProjectMemberResponseDTO response = projectMemberService.addMember(projectId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping 
    public ResponseEntity<List<ProjectMemberResponseDTO>> listMembers(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectMemberService.listMembers(projectId));
    }
}
