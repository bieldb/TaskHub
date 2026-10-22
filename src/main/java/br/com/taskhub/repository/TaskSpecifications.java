package br.com.taskhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.com.taskhub.entity.Priority;
import br.com.taskhub.entity.Status;
import br.com.taskhub.entity.Task;

public final class TaskSpecifications {

    private TaskSpecifications() {

    }
    public static Specification<Task> belongsToProject(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }
    public static Specification<Task> hasStatus(Status status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
    public static Specification<Task> hasPriority(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }
    public static Specification<Task> hasAssignee(UUID assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }
}
