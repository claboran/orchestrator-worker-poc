--liquibase formatted sql
--changeset lab:1
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE job_state (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id VARCHAR(255) NOT NULL UNIQUE,
  status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
  payload JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);
