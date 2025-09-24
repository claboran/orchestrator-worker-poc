--liquibase formatted sql
--changeset lab:1
CREATE TABLE job_state (
  id BIGSERIAL PRIMARY KEY,
  job_id VARCHAR(255) NOT NULL UNIQUE,
  status VARCHAR(50) NOT NULL,3
  payload JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

--changeset lab:1 rollback
DROP TABLE job_state;
