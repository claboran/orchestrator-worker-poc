--liquibase formatted sql
--changeset lab:2
CREATE TABLE page_state (
  id uuid PRIMARY KEY,
  job_state_id uuid NOT NULL REFERENCES job_state(id),
  status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
  data JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

