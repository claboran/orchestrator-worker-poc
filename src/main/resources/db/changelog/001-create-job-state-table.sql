--liquibase formatted sql
--changeset lab:1

CREATE TABLE job_state (
  id uuid PRIMARY KEY,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE
);
