package br.com.taskhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.taskhub.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

}
