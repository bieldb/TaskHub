package br.com.taskhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.taskhub.entity.Task;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

}
