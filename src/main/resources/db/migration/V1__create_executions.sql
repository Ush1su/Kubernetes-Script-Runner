CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE executions (
    id UUID PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('QUEUED','IN_PROGRESS','FINISHED')),
    script TEXT NOT NULL,

    requested_cpu TEXT,
    requested_memory_mb TEXT,

    job_name TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    outcome TEXT
);

CREATE INDEX idx_executions_status_created_at ON executions(status, created_at DESC);