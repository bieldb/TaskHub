CREATE TABLE
    tasks (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
        title VARCHAR(150) NOT NULL,
        description VARCHAR(1000),
        status VARCHAR(20) NOT NULL,
        priority VARCHAR(20) NOT NULL,
        deadline DATE,
        project_id UUID NOT NULL,
        CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
        CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
        CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
    );

CREATE INDEX idx_tasks_project_id ON tasks (project_id);