ALTER TABLE tasks
    ADD COLUMN assignee_id UUID NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE SET NULL;