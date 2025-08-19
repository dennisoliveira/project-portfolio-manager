-- ===========================
-- V1 - Initial schema
-- ===========================

CREATE TABLE project (
  id                   BIGSERIAL      PRIMARY KEY,
  name                 VARCHAR(150)   NOT NULL,
  start_date           DATE           NOT NULL,
  expected_end_date    DATE           NOT NULL,
  actual_end_date      DATE,
  total_budget         NUMERIC(15,2)  NOT NULL,
  description          TEXT,
  manager_external_id  VARCHAR(100)   NOT NULL,
  status               VARCHAR(30)    NOT NULL,
  risk                 VARCHAR(10)    NOT NULL,
  created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
  updated_at           TIMESTAMP      NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_project_status CHECK (status IN (
    'EM_ANALISE','ANALISE_REALIZADA','ANALISE_APROVADA',
    'INICIADO','PLANEJADO','EM_ANDAMENTO','ENCERRADO','CANCELADO'
  )),
  CONSTRAINT chk_project_risk CHECK (risk IN ('BAIXO','MEDIO','ALTO')),
  CONSTRAINT chk_project_budget_positive CHECK (total_budget > 0),
  CONSTRAINT chk_project_dates_order CHECK (expected_end_date >= start_date)
);

CREATE INDEX ix_project_status             ON project (status);
CREATE INDEX ix_project_manager_external   ON project (manager_external_id);
CREATE INDEX ix_project_start_date         ON project (start_date);
CREATE INDEX ix_project_expected_date      ON project (expected_end_date);
CREATE INDEX ix_project_total_budget       ON project (total_budget);

CREATE TABLE project_member (
  project_id          BIGINT        NOT NULL,
  member_external_id  VARCHAR(100)  NOT NULL,
  created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),

  CONSTRAINT project_member_pkey PRIMARY KEY (project_id, member_external_id),

  CONSTRAINT fk_pm_project FOREIGN KEY (project_id)
    REFERENCES project (id) ON DELETE CASCADE
);

CREATE INDEX ix_pm_member_external ON project_member (member_external_id);