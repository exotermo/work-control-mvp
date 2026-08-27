DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS credentials_metadata;
DROP TABLE IF EXISTS artifacts;
DROP TABLE IF EXISTS approvals;
DROP TABLE IF EXISTS execution_events;
DROP TABLE IF EXISTS executions;
DROP TABLE IF EXISTS task_steps;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS agents;
DROP TABLE IF EXISTS device_capabilities;
DROP TABLE IF EXISTS devices;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS workspace_members;
DROP TABLE IF EXISTS workspaces;
DROP TABLE IF EXISTS users;

DROP TYPE IF EXISTS approval_decision;
DROP TYPE IF EXISTS execution_node_status;
DROP TYPE IF EXISTS device_status;
DROP TYPE IF EXISTS agent_runtime_status;
DROP TYPE IF EXISTS agent_role;
DROP TYPE IF EXISTS task_status;

DROP FUNCTION IF EXISTS set_updated_at();
