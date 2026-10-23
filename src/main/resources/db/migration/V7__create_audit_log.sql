CREATE TABLE
    audit_log (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
        entity_type VARCHAR(30) NOT NULL,
        entity_id UUID NOT NULL,
        action VARCHAR(10) NOT NULL,
        actor VARCHAR(100) NOT NULL,
        old_value JSONB,
        new_value JSONB,
        occurred_at TIMESTAMP NOT NULL DEFAULT now (),
        CONSTRAINT ck_audit_log_entity_type CHECK (entity_type IN ('PROJECT', 'TASK', 'USER', 'PROJECT_MEMBER')),
        CONSTRAINT ck_audit_log_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
    );

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id, occurred_at DESC);
