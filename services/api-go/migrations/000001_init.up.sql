-- Schema inicial do Work Control. Ver docs/sdd/README.md e o README raiz (seção "Banco de dados")
-- para o desenho de entidades original; este migration é a versão executável dele.

CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TYPE task_status           AS ENUM ('QUEUED','IN_PROGRESS','COMPLETED','FAILED');
CREATE TYPE agent_role            AS ENUM ('DEV','QA','DEVOPS');
CREATE TYPE agent_runtime_status  AS ENUM ('RUNNING','WARNING','IDLE','ERROR','OFFLINE');
CREATE TYPE device_status         AS ENUM ('ONLINE','OFFLINE');
CREATE TYPE execution_node_status AS ENUM ('DONE','ACTIVE','PENDING');
CREATE TYPE approval_decision     AS ENUM ('APPROVED','REJECTED');

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL UNIQUE,
  display_name text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspaces (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  slug text NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspace_members (
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role text NOT NULL DEFAULT 'member',
  joined_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, user_id)
);

CREATE TABLE projects (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  name text NOT NULL,
  slug text NOT NULL,
  repo_url text,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, slug)
);

CREATE TABLE devices (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  name text NOT NULL,
  role_label text NOT NULL,
  status device_status NOT NULL DEFAULT 'OFFLINE',
  cpu_percent smallint NOT NULL DEFAULT 0 CHECK (cpu_percent BETWEEN 0 AND 100),
  ram_percent smallint NOT NULL DEFAULT 0 CHECK (ram_percent BETWEEN 0 AND 100),
  disk_percent smallint NOT NULL DEFAULT 0 CHECK (disk_percent BETWEEN 0 AND 100),
  last_heartbeat_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_devices_workspace_status ON devices(workspace_id, status);

CREATE TABLE device_capabilities (
  device_id uuid NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  capability text NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  PRIMARY KEY (device_id, capability)
);

CREATE TABLE agents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  name text NOT NULL,
  role agent_role NOT NULL,
  model text NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE tasks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  code text NOT NULL,
  title text NOT NULL,
  description text NOT NULL DEFAULT '',
  status task_status NOT NULL DEFAULT 'QUEUED',
  progress_percent int NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
  created_by_user_id uuid REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (project_id, code)
);
CREATE INDEX idx_tasks_project_created ON tasks(project_id, created_at DESC);
CREATE TRIGGER trg_tasks_updated_at BEFORE UPDATE ON tasks
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE task_steps (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  sequence_order int NOT NULL,
  label text NOT NULL,
  status execution_node_status NOT NULL DEFAULT 'PENDING',
  agent_id uuid REFERENCES agents(id),
  UNIQUE (task_id, sequence_order)
);
CREATE INDEX idx_task_steps_task ON task_steps(task_id);

CREATE TABLE executions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  task_step_id uuid NOT NULL REFERENCES task_steps(id) ON DELETE CASCADE,
  agent_id uuid NOT NULL REFERENCES agents(id),
  device_id uuid REFERENCES devices(id),
  attempt_number int NOT NULL DEFAULT 1,
  status agent_runtime_status NOT NULL DEFAULT 'IDLE',
  progress_percent int CHECK (progress_percent BETWEEN 0 AND 100),
  current_activity text NOT NULL DEFAULT '',
  started_at timestamptz NOT NULL DEFAULT now(),
  finished_at timestamptz
);
CREATE INDEX idx_executions_task_step ON executions(task_step_id);
CREATE INDEX idx_executions_agent_status ON executions(agent_id, status);

CREATE TABLE execution_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  execution_id uuid REFERENCES executions(id),
  agent_id uuid REFERENCES agents(id),
  event_type text NOT NULL,
  message text NOT NULL,
  highlighted boolean NOT NULL DEFAULT false,
  occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_execution_events_task_time ON execution_events(task_id, occurred_at DESC);
CREATE INDEX idx_execution_events_execution ON execution_events(execution_id) WHERE execution_id IS NOT NULL;

CREATE TABLE approvals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  task_id uuid REFERENCES tasks(id),
  execution_id uuid REFERENCES executions(id),
  requested_by_agent_id uuid REFERENCES agents(id),
  title text NOT NULL,
  description text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}',
  decision approval_decision,
  decided_by_user_id uuid REFERENCES users(id),
  decided_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_approvals_pending ON approvals(workspace_id) WHERE decision IS NULL;

CREATE TABLE artifacts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id uuid NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  execution_id uuid REFERENCES executions(id),
  kind text NOT NULL,
  file_path text,
  storage_url text,
  content_preview text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_artifacts_task ON artifacts(task_id);

CREATE TABLE credentials_metadata (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  device_id uuid REFERENCES devices(id),
  name text NOT NULL,
  kind text NOT NULL,
  external_ref text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  rotated_at timestamptz
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  user_id uuid REFERENCES users(id),
  agent_id uuid REFERENCES agents(id),
  device_id uuid REFERENCES devices(id),
  approval_id uuid REFERENCES approvals(id),
  action text NOT NULL,
  result text NOT NULL,
  metadata jsonb NOT NULL DEFAULT '{}',
  occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_workspace_time ON audit_logs(workspace_id, occurred_at DESC);
