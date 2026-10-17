CREATE TABLE
    projects (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
        name VARCHAR(100) NOT NULL,
        description VARCHAR(500),
        created_at TIMESTAMP NOT NULL DEFAULT now ()
    );