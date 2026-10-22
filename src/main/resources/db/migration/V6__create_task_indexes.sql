CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);

CREATE INDEX idx_tasks_project_priority ON tasks (project_id, priority);

CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);

DROP INDEX idx_tasks_project_id;