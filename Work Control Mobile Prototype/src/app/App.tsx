import { useState, type FC } from "react";
import {
  Home, ListTodo, Plus, Server, FolderOpen,
  ChevronRight, ChevronLeft, AlertTriangle,
  CheckCircle, XCircle, Terminal, FileCode,
  Activity, Bot, Monitor, Clock, Wifi,
  Send, Upload, RefreshCw, Zap,
  Box, Shield, Layers, Globe, Lock,
  Bug, Code2, PlayCircle,
} from "lucide-react";

type Screen =
  | "home" | "tasks" | "task-detail"
  | "agent-dev" | "agent-qa" | "agent-devops"
  | "code-diff" | "terminal" | "approval"
  | "machines" | "machine-detail" | "new-task"
  | "files" | "result";

type StatusType = "online" | "running" | "warning" | "error" | "idle" | "offline";

function getActiveNav(s: Screen): string {
  if (["machines", "machine-detail"].includes(s)) return "machines";
  if (["files"].includes(s)) return "files";
  if (["tasks", "task-detail", "agent-dev", "agent-qa", "agent-devops", "code-diff", "terminal", "approval", "result"].includes(s)) return "tasks";
  return "home";
}

export default function App() {
  const [screen, setScreen] = useState<Screen>("home");
  const [history, setHistory] = useState<Screen[]>([]);
  const [plusOpen, setPlusOpen] = useState(false);
  const [newTaskText, setNewTaskText] = useState("");
  const [approvalDone, setApprovalDone] = useState<"approved" | "rejected" | null>(null);

  const go = (s: Screen) => {
    setHistory((h) => [...h, screen]);
    setScreen(s);
    setPlusOpen(false);
  };

  const back = () => {
    if (history.length > 0) {
      setScreen(history[history.length - 1]);
      setHistory((h) => h.slice(0, -1));
    }
  };

  const showBack = history.length > 0;

  // ── Shared micro-components ──────────────────────────────────────────────

  const StatusDot = ({ status }: { status: StatusType }) => {
    const cls: Record<StatusType, string> = {
      online: "bg-green-400",
      running: "bg-green-400 animate-pulse",
      warning: "bg-yellow-400 animate-pulse",
      error: "bg-red-500",
      idle: "bg-blue-400",
      offline: "bg-neutral-600",
    };
    return <span className={`inline-block w-2 h-2 rounded-full shrink-0 ${cls[status]}`} />;
  };

  const Bar = ({
    value,
    color = "violet",
  }: {
    value: number;
    color?: "violet" | "green" | "yellow" | "red" | "blue";
  }) => {
    const cls: Record<string, string> = {
      violet: "bg-violet-500",
      green: "bg-green-500",
      yellow: "bg-yellow-500",
      red: "bg-red-500",
      blue: "bg-blue-500",
    };
    return (
      <div className="h-1 bg-white/10 rounded-full overflow-hidden">
        <div className={`h-full ${cls[color]} rounded-full`} style={{ width: `${value}%` }} />
      </div>
    );
  };

  const TopBar = ({ title, subtitle }: { title: string; subtitle?: string }) => (
    <div className="flex items-center gap-3 px-4 py-4 border-b border-white/[0.06] shrink-0">
      {showBack && (
        <button onClick={back} className="text-neutral-500 hover:text-white transition-colors">
          <ChevronLeft size={20} />
        </button>
      )}
      <div className="flex-1 min-w-0">
        <h1 className="text-white font-semibold text-[15px] leading-tight truncate">{title}</h1>
        {subtitle && (
          <p className="text-neutral-600 text-[10px] mt-0.5" style={{ fontFamily: "'JetBrains Mono', monospace" }}>
            {subtitle}
          </p>
        )}
      </div>
    </div>
  );

  const NavBar = () => {
    const active = getActiveNav(screen);
    return (
      <div className="flex items-end border-t border-white/[0.06] bg-[#09090e] px-2 pb-2 shrink-0">
        {([
          { id: "home", label: "Início", Icon: Home },
          { id: "tasks", label: "Tarefas", Icon: ListTodo },
          { id: "_plus" as const, label: "", Icon: Plus },
          { id: "machines", label: "Máquinas", Icon: Server },
          { id: "files", label: "Arquivos", Icon: FolderOpen },
        ] as { id: string; label: string; Icon: FC<{ size: number; className?: string }> }[]).map(({ id, label, Icon }) => {
          if (id === "_plus") {
            return (
              <button
                key={id}
                onClick={() => setPlusOpen(true)}
                className="flex-1 flex flex-col items-center pt-2 pb-1"
              >
                <div className="w-12 h-12 rounded-full bg-violet-600 flex items-center justify-center shadow-lg shadow-violet-900/50 -mt-4 border-4 border-[#09090e]">
                  <Plus size={22} className="text-white" />
                </div>
              </button>
            );
          }
          const isActive = active === id;
          return (
            <button
              key={id}
              onClick={() => go(id as Screen)}
              className="flex-1 flex flex-col items-center gap-1 pt-2 pb-1"
            >
              <Icon size={20} className={isActive ? "text-violet-400" : "text-neutral-700"} />
              <span
                className={`text-[9px] font-medium ${isActive ? "text-violet-400" : "text-neutral-700"}`}
              >
                {label}
              </span>
            </button>
          );
        })}
      </div>
    );
  };

  const PlusMenu = () => (
    <div className="absolute inset-0 z-50 flex flex-col justify-end">
      <div
        className="absolute inset-0 bg-black/70"
        onClick={() => setPlusOpen(false)}
      />
      <div className="relative bg-[#0f0f18] border-t border-white/[0.08] rounded-t-3xl px-4 pt-3 pb-6">
        <div className="w-8 h-1 bg-white/15 rounded-full mx-auto mb-5" />
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3 px-1"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Ações rápidas
        </p>
        {([
          { icon: Zap, label: "Nova tarefa", color: "text-violet-400", action: () => go("new-task") },
          { icon: Bot, label: "Perguntar à IA", color: "text-blue-400", action: () => setPlusOpen(false) },
          { icon: Terminal, label: "Terminal / SSH", color: "text-green-400", action: () => go("terminal") },
          { icon: Upload, label: "Enviar arquivo", color: "text-yellow-400", action: () => setPlusOpen(false) },
          { icon: Monitor, label: "Acessar máquina", color: "text-neutral-300", action: () => go("machine-detail") },
        ] as { icon: FC<{ size: number; className?: string }>; label: string; color: string; action: () => void }[]).map(({ icon: Icon, label, color, action }) => (
          <button
            key={label}
            onClick={action}
            className="w-full flex items-center gap-4 px-2 py-3.5 border-b border-white/[0.05] last:border-0 rounded-xl active:bg-white/5 transition-colors"
          >
            <div className="w-10 h-10 rounded-2xl bg-white/[0.05] flex items-center justify-center shrink-0">
              <Icon size={18} className={color} />
            </div>
            <span className="text-white text-[14px] flex-1 text-left">{label}</span>
            <ChevronRight size={14} className="text-neutral-700" />
          </button>
        ))}
      </div>
    </div>
  );

  // ── SCREENS ──────────────────────────────────────────────────────────────

  const HomeScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      {/* Header */}
      <div className="px-4 pt-5 pb-4 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="w-1.5 h-1.5 rounded-full bg-green-400 inline-block" />
            <span
              className="text-neutral-500 text-[11px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Escritório · Projeto Atlas
            </span>
          </div>
          <h1 className="text-white text-[22px] font-bold tracking-tight">Work Control</h1>
        </div>
        <button className="w-9 h-9 rounded-2xl bg-white/[0.05] border border-white/[0.08] flex items-center justify-center">
          <RefreshCw size={15} className="text-neutral-500" />
        </button>
      </div>

      {/* Approval banner */}
      <button
        onClick={() => go("approval")}
        className="mx-4 mb-5 w-[calc(100%-2rem)] flex items-center gap-3 px-4 py-3 bg-yellow-500/8 border border-yellow-500/20 rounded-2xl"
      >
        <AlertTriangle size={15} className="text-yellow-400 shrink-0" />
        <span className="text-yellow-300 text-[13px] font-medium flex-1 text-left">
          1 aprovação pendente · Agent DevOps
        </span>
        <ChevronRight size={14} className="text-yellow-600" />
      </button>

      {/* Active agents */}
      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Agentes Ativos · 3
        </p>
        <div className="space-y-2.5">
          {/* Dev */}
          <button
            onClick={() => go("agent-dev")}
            className="w-full bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4 text-left active:scale-[0.99] transition-transform"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-violet-600/20 border border-violet-500/20 flex items-center justify-center">
                  <Bot size={16} className="text-violet-400" />
                </div>
                <div>
                  <p className="text-white text-[13px] font-semibold">Agent Dev</p>
                  <p
                    className="text-neutral-600 text-[10px]"
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    Workstation-01
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <StatusDot status="running" />
                <span
                  className="text-green-400 text-[12px] font-semibold"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  68%
                </span>
              </div>
            </div>
            <p className="text-neutral-500 text-[12px] mb-2.5">
              Corrigindo bug #184 · auth.service.ts
            </p>
            <Bar value={68} color="violet" />
          </button>

          {/* QA */}
          <button
            onClick={() => go("agent-qa")}
            className="w-full bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4 text-left active:scale-[0.99] transition-transform"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-blue-600/20 border border-blue-500/20 flex items-center justify-center">
                  <Bot size={16} className="text-blue-400" />
                </div>
                <div>
                  <p className="text-white text-[13px] font-semibold">Agent QA</p>
                  <p
                    className="text-neutral-600 text-[10px]"
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    Server-Atlas
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <StatusDot status="running" />
                <span
                  className="text-green-400 text-[12px] font-semibold"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  66%
                </span>
              </div>
            </div>
            <p className="text-neutral-500 text-[12px] mb-2.5">Executando testes · 21/32 concluídos</p>
            <Bar value={66} color="green" />
          </button>

          {/* DevOps */}
          <button
            onClick={() => go("agent-devops")}
            className="w-full bg-[#0f0f18] border border-yellow-500/15 rounded-2xl p-4 text-left active:scale-[0.99] transition-transform"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-yellow-600/15 border border-yellow-500/20 flex items-center justify-center">
                  <Bot size={16} className="text-yellow-400" />
                </div>
                <div>
                  <p className="text-white text-[13px] font-semibold">Agent DevOps</p>
                  <p
                    className="text-neutral-600 text-[10px]"
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    Server-CI
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <StatusDot status="warning" />
                <span
                  className="text-yellow-400 text-[11px] font-semibold"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  WAIT
                </span>
              </div>
            </div>
            <p className="text-yellow-500/70 text-[12px]">Aguardando aprovação de deploy</p>
          </button>
        </div>
      </div>

      {/* Machines */}
      <div className="px-4 mb-5">
        <div className="flex items-center justify-between mb-3">
          <p
            className="text-neutral-600 text-[10px] uppercase tracking-widest"
            style={{ fontFamily: "'JetBrains Mono', monospace" }}
          >
            Máquinas
          </p>
          <button onClick={() => go("machines")} className="text-violet-400 text-[12px]">
            Ver todas
          </button>
        </div>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { name: "Workstation-01", role: "Dev Machine", cpu: 45, ram: 62 },
            { name: "Server-Atlas", role: "App Server", cpu: 23, ram: 41 },
          ].map((m, i) => (
            <button
              key={m.name}
              onClick={() => go("machine-detail")}
              className={`w-full flex items-center gap-3 px-4 py-3.5 active:bg-white/[0.02] ${i > 0 ? "border-t border-white/[0.06]" : ""}`}
            >
              <StatusDot status="online" />
              <div className="flex-1 text-left">
                <p className="text-white text-[13px] font-medium">{m.name}</p>
                <p
                  className="text-neutral-600 text-[10px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  {m.role}
                </p>
              </div>
              <div className="text-right mr-2">
                <p
                  className="text-neutral-500 text-[10px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  CPU <span className="text-green-400">{m.cpu}%</span>
                </p>
                <p
                  className="text-neutral-500 text-[10px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  RAM <span className="text-blue-400">{m.ram}%</span>
                </p>
              </div>
              <ChevronRight size={14} className="text-neutral-700" />
            </button>
          ))}
        </div>
      </div>

      {/* Recent tasks */}
      <div className="px-4 mb-6">
        <div className="flex items-center justify-between mb-3">
          <p
            className="text-neutral-600 text-[10px] uppercase tracking-widest"
            style={{ fontFamily: "'JetBrains Mono', monospace" }}
          >
            Tarefas Recentes
          </p>
          <button onClick={() => go("tasks")} className="text-violet-400 text-[12px]">
            Ver todas
          </button>
        </div>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          <button
            onClick={() => go("task-detail")}
            className="w-full flex items-center gap-3 px-4 py-3.5 border-b border-white/[0.06] active:bg-white/[0.02]"
          >
            <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse shrink-0" />
            <div className="flex-1 text-left">
              <p className="text-white text-[13px] font-medium">Investigar bug #184</p>
              <p className="text-neutral-500 text-[11px]">3 agentes · Em progresso</p>
            </div>
            <span
              className="text-violet-400 text-[12px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              68%
            </span>
          </button>
          <button
            onClick={() => go("result")}
            className="w-full flex items-center gap-3 px-4 py-3.5 active:bg-white/[0.02]"
          >
            <div className="w-2 h-2 rounded-full bg-neutral-700 shrink-0" />
            <div className="flex-1 text-left">
              <p className="text-white text-[13px] font-medium">Refactor auth module</p>
              <p className="text-neutral-500 text-[11px]">2 agentes · Concluído 2h atrás</p>
            </div>
            <CheckCircle size={14} className="text-green-500" />
          </button>
        </div>
      </div>
    </div>
  );

  // ── Task List ─────────────────────────────────────────────────────────────
  const TaskListScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Tarefas" subtitle="PROJETO ATLAS · 5 TOTAL" />
      <div className="px-4 pt-4">
        <div
          className="flex gap-2 mb-4 overflow-x-auto pb-1"
          style={{ scrollbarWidth: "none" }}
        >
          {["Todas", "Ativas", "Em fila", "Concluídas"].map((tab, i) => (
            <button
              key={tab}
              className={`shrink-0 px-3 py-1.5 rounded-lg text-[12px] font-medium ${i === 0 ? "bg-violet-600 text-white" : "bg-[#16162a] text-neutral-500"}`}
            >
              {tab}
            </button>
          ))}
        </div>
        <div className="space-y-2.5 pb-4">
          {[
            { id: "#184", title: "Investigar e corrigir bug #184", status: "Em progresso", agents: 3, progress: 68 },
            { id: "#183", title: "Adicionar rate limiting na API", status: "Em fila", agents: 0, progress: 0 },
            { id: "#182", title: "Refactor módulo de autenticação", status: "Concluído", agents: 2, progress: 100 },
            { id: "#181", title: "Migração PostgreSQL → Supabase", status: "Concluído", agents: 2, progress: 100 },
            { id: "#179", title: "Configurar CI/CD pipeline", status: "Falhou", agents: 1, progress: 45 },
          ].map((task) => (
            <button
              key={task.id}
              onClick={() => go("task-detail")}
              className="w-full bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4 text-left active:scale-[0.99] transition-transform"
            >
              <div className="flex items-start justify-between mb-2">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1.5">
                    <span
                      className="text-neutral-700 text-[10px]"
                      style={{ fontFamily: "'JetBrains Mono', monospace" }}
                    >
                      {task.id}
                    </span>
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full ${
                        task.status === "Em progresso"
                          ? "bg-green-500/10 text-green-400"
                          : task.status === "Concluído"
                          ? "bg-neutral-700/40 text-neutral-500"
                          : task.status === "Em fila"
                          ? "bg-blue-500/10 text-blue-400"
                          : "bg-red-500/10 text-red-400"
                      }`}
                      style={{ fontFamily: "'JetBrains Mono', monospace" }}
                    >
                      {task.status}
                    </span>
                  </div>
                  <p className="text-white text-[13px] font-medium leading-snug">{task.title}</p>
                </div>
                <ChevronRight size={14} className="text-neutral-700 mt-1 ml-2 shrink-0" />
              </div>
              {task.progress > 0 && task.progress < 100 && (
                <div className="mt-3">
                  <div className="flex justify-between mb-1.5">
                    <span className="text-neutral-600 text-[10px]">{task.agents} agentes ativos</span>
                    <span
                      className="text-violet-400 text-[10px]"
                      style={{ fontFamily: "'JetBrains Mono', monospace" }}
                    >
                      {task.progress}%
                    </span>
                  </div>
                  <Bar value={task.progress} />
                </div>
              )}
            </button>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Task Detail ───────────────────────────────────────────────────────────
  const TaskDetailScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Bug #184: Crash no login" subtitle="INVESTIGAR E CORRIGIR" />

      <div className="px-4 pt-4 mb-5">
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4">
          <div className="flex items-center justify-between mb-3">
            <div>
              <p className="text-white font-semibold">Em progresso</p>
              <p
                className="text-neutral-600 text-[11px] mt-0.5"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                Iniciado há 42 min · 3 agentes
              </p>
            </div>
            <span
              className="text-3xl font-bold text-violet-400"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              68%
            </span>
          </div>
          <Bar value={68} />
        </div>
      </div>

      {/* Flow diagram */}
      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Fluxo de Execução
        </p>
        <div className="overflow-x-auto pb-2 -mx-4 px-4" style={{ scrollbarWidth: "none" }}>
          <div className="flex items-center min-w-max gap-0">
            {([
              { label: "Usuário", icon: Shield, status: "done" },
              { label: "Orch.", icon: Layers, status: "done" },
              { label: "Agent Dev", icon: Bot, status: "active", onTap: () => go("agent-dev") },
              { label: "Agent QA", icon: Bot, status: "pending", onTap: () => go("agent-qa") },
              { label: "DevOps", icon: Bot, status: "pending", onTap: () => go("agent-devops") },
            ] as { label: string; icon: FC<{ size: number; className?: string }>; status: string; onTap?: () => void }[]).map((node, i) => (
              <div key={node.label} className="flex items-center">
                {i > 0 && (
                  <div
                    className={`w-5 h-px mx-1 ${node.status !== "pending" ? "bg-violet-500" : "bg-neutral-800"}`}
                  />
                )}
                <button
                  onClick={node.onTap}
                  className={`flex flex-col items-center gap-1.5 px-3 py-2.5 rounded-2xl border transition-all ${
                    node.status === "active"
                      ? "bg-violet-600/20 border-violet-500/40"
                      : node.status === "done"
                      ? "bg-white/[0.03] border-white/[0.08]"
                      : "bg-transparent border-white/[0.04]"
                  }`}
                >
                  <div
                    className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                      node.status === "active"
                        ? "bg-violet-600"
                        : node.status === "done"
                        ? "bg-green-600/25"
                        : "bg-white/[0.04]"
                    }`}
                  >
                    {node.status === "done" ? (
                      <CheckCircle size={16} className="text-green-400" />
                    ) : (
                      <node.icon
                        size={15}
                        className={node.status === "active" ? "text-white" : "text-neutral-700"}
                      />
                    )}
                  </div>
                  <p
                    className={`text-[9px] ${
                      node.status === "active"
                        ? "text-violet-300"
                        : node.status === "done"
                        ? "text-neutral-500"
                        : "text-neutral-700"
                    }`}
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    {node.label}
                  </p>
                  {node.status === "active" && (
                    <div className="w-1 h-1 rounded-full bg-violet-400 animate-pulse" />
                  )}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Agents */}
      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Agentes
        </p>
        <div className="space-y-2">
          {[
            { name: "Agent Dev", task: "Modificando auth.service.ts", status: "running" as StatusType, progress: 68, dest: "agent-dev" as Screen },
            { name: "Agent QA", task: "Aguardando Agent Dev", status: "idle" as StatusType, progress: 0, dest: "agent-qa" as Screen },
            { name: "Agent DevOps", task: "Aguardando aprovação", status: "warning" as StatusType, progress: 85, dest: "agent-devops" as Screen },
          ].map((agent) => (
            <button
              key={agent.name}
              onClick={() => go(agent.dest)}
              className="w-full bg-[#0f0f18] border border-white/[0.06] rounded-2xl px-4 py-3.5 flex items-center gap-3 text-left active:bg-white/[0.02]"
            >
              <StatusDot status={agent.status} />
              <div className="flex-1">
                <p className="text-white text-[13px] font-medium">{agent.name}</p>
                <p
                  className="text-neutral-600 text-[11px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  {agent.task}
                </p>
              </div>
              {agent.progress > 0 && (
                <span
                  className="text-[11px] text-violet-400"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  {agent.progress}%
                </span>
              )}
              <ChevronRight size={14} className="text-neutral-700" />
            </button>
          ))}
        </div>
      </div>

      {/* Activity log */}
      <div className="px-4 mb-6">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Atividade Recente
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { time: "14:53", text: "Agent Dev: patch aplicado em auth.service.ts", ok: true },
            { time: "14:51", text: "Agent Dev: analisando stack trace linha 284", ok: false },
            { time: "14:47", text: "Agent Dev: auth.service.ts aberto", ok: false },
            { time: "14:41", text: "Orquestrador: Agent Dev iniciado", ok: false },
            { time: "14:40", text: "Tarefa criada pelo usuário", ok: false },
          ].map((log, i) => (
            <div
              key={i}
              className={`flex gap-3 px-4 py-3 ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              <span
                className="text-neutral-700 text-[10px] shrink-0 mt-0.5"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {log.time}
              </span>
              <p
                className={`text-[11px] ${log.ok ? "text-green-400" : "text-neutral-500"}`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {log.text}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Agent Dev ─────────────────────────────────────────────────────────────
  const AgentDevScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Agent Dev" subtitle="WORKSTATION-01 · RUNNING" />

      <div className="px-4 pt-4 mb-5">
        <div className="bg-[#0f0f18] border border-violet-500/20 rounded-2xl p-4">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-11 h-11 rounded-2xl bg-violet-600/20 border border-violet-500/25 flex items-center justify-center">
              <Bot size={20} className="text-violet-400" />
            </div>
            <div className="flex-1">
              <p className="text-white font-semibold">Agent Dev</p>
              <p
                className="text-neutral-600 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                Claude Sonnet 4.5 · Workstation-01
              </p>
            </div>
            <div className="flex flex-col items-end gap-1">
              <StatusDot status="running" />
              <span
                className="text-green-400 text-[9px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                ACTIVE
              </span>
            </div>
          </div>
          <div className="bg-black/30 rounded-xl px-3 py-3 border border-white/[0.04]">
            <p
              className="text-neutral-600 text-[9px] uppercase tracking-wider mb-1.5"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Objetivo
            </p>
            <p
              className="text-neutral-300 text-[12px] leading-relaxed"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Investigar e corrigir bug #184 no módulo de autenticação que causa crash no login
            </p>
          </div>
        </div>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Atividade Atual
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <p
              className="text-green-400 text-[12px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Aplicando patch
            </p>
          </div>
          <p className="text-neutral-400 text-[12px] leading-relaxed">
            Corrigindo tratamento de token expirado na linha 284. Adicionando verificação de nulo e
            exceção estruturada.
          </p>
          <div className="mt-3.5">
            <Bar value={68} />
            <div className="flex justify-between mt-1.5">
              <span
                className="text-neutral-700 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                42 min decorridos
              </span>
              <span
                className="text-violet-400 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                68%
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Arquivos Modificados · 3
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { file: "auth.service.ts", path: "src/auth/", changes: "+12 -3" },
            { file: "auth.types.ts", path: "src/auth/", changes: "+4 -0" },
            { file: "auth.test.spec.ts", path: "src/auth/__tests__/", changes: "+28 -0" },
          ].map((f, i) => (
            <button
              key={f.file}
              onClick={() => go("code-diff")}
              className={`w-full flex items-center gap-3 px-4 py-3.5 active:bg-white/[0.02] ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              <FileCode size={14} className="text-violet-400 shrink-0" />
              <div className="flex-1 text-left">
                <p
                  className="text-white text-[12px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  {f.file}
                </p>
                <p
                  className="text-neutral-700 text-[10px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  {f.path}
                </p>
              </div>
              <span
                className="text-blue-400 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {f.changes}
              </span>
            </button>
          ))}
        </div>
      </div>

      <div className="px-4 mb-6">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Ações
        </p>
        <div className="grid grid-cols-3 gap-2">
          {[
            { label: "Código", Icon: Code2, action: () => go("code-diff") },
            { label: "Terminal", Icon: Terminal, action: () => go("terminal") },
            { label: "Pausar", Icon: PlayCircle, action: () => {} },
          ].map(({ label, Icon, action }) => (
            <button
              key={label}
              onClick={action}
              className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl py-4 flex flex-col items-center gap-2 active:bg-white/[0.03]"
            >
              <Icon size={18} className="text-neutral-500" />
              <span className="text-neutral-500 text-[11px]">{label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Agent QA ──────────────────────────────────────────────────────────────
  const AgentQAScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Agent QA" subtitle="SERVER-ATLAS · AGUARDANDO" />

      <div className="px-4 pt-4 mb-5">
        <div className="bg-[#0f0f18] border border-blue-500/20 rounded-2xl p-4">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-11 h-11 rounded-2xl bg-blue-600/20 border border-blue-500/25 flex items-center justify-center">
              <Bot size={20} className="text-blue-400" />
            </div>
            <div className="flex-1">
              <p className="text-white font-semibold">Agent QA</p>
              <p
                className="text-neutral-600 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                GPT-4o · Server-Atlas
              </p>
            </div>
            <div className="flex flex-col items-end gap-1">
              <StatusDot status="idle" />
              <span
                className="text-blue-400 text-[9px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                IDLE
              </span>
            </div>
          </div>
          <div className="bg-blue-500/5 rounded-xl px-3 py-2.5 border border-blue-500/10">
            <p
              className="text-blue-300/60 text-[11px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Aguardando patch do Agent Dev para iniciar suite completa
            </p>
          </div>
        </div>
      </div>

      <div className="px-4 mb-6">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Suites de Teste · 21/32
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { suite: "auth.service.spec", passed: 8, total: 8, done: true },
            { suite: "auth.guards.spec", passed: 6, total: 6, done: true },
            { suite: "auth.integration.spec", passed: 7, total: 7, done: true },
            { suite: "auth.e2e.spec", passed: 0, total: 11, done: false },
          ].map((s, i) => (
            <div
              key={s.suite}
              className={`flex items-center gap-3 px-4 py-3.5 ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              {s.done ? (
                <CheckCircle size={14} className="text-green-400 shrink-0" />
              ) : (
                <div className="w-3.5 h-3.5 rounded-full border border-neutral-700 shrink-0" />
              )}
              <span
                className={`flex-1 text-[11px] ${s.done ? "text-neutral-400" : "text-neutral-700"}`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {s.suite}
              </span>
              <span
                className={`text-[10px] ${s.done ? "text-green-400" : "text-neutral-700"}`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {s.passed}/{s.total}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Agent DevOps ──────────────────────────────────────────────────────────
  const AgentDevOpsScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Agent DevOps" subtitle="SERVER-CI · AGUARDANDO APROVAÇÃO" />

      <div className="px-4 pt-4 mb-5">
        <div className="bg-[#0f0f18] border border-yellow-500/20 rounded-2xl p-4">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-11 h-11 rounded-2xl bg-yellow-600/15 border border-yellow-500/20 flex items-center justify-center">
              <Bot size={20} className="text-yellow-400" />
            </div>
            <div className="flex-1">
              <p className="text-white font-semibold">Agent DevOps</p>
              <p
                className="text-neutral-600 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                Claude Haiku 4.5 · Server-CI
              </p>
            </div>
            <StatusDot status="warning" />
          </div>
          <button
            onClick={() => go("approval")}
            className="w-full bg-yellow-500/8 border border-yellow-500/20 rounded-xl px-3 py-3 flex items-center gap-3"
          >
            <AlertTriangle size={15} className="text-yellow-400 shrink-0" />
            <p
              className="text-yellow-300 text-[12px] flex-1 text-left"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Aprovação necessária para deploy
            </p>
            <ChevronRight size={13} className="text-yellow-600" />
          </button>
        </div>
      </div>

      <div className="px-4 mb-6">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Pipeline CI/CD
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { step: "Build", status: "done", time: "1m 32s" },
            { step: "Lint & Tests", status: "done", time: "3m 18s" },
            { step: "Docker Image", status: "done", time: "2m 05s" },
            { step: "Deploy Staging", status: "pending", time: "—" },
            { step: "Smoke Tests", status: "locked", time: "—" },
          ].map((step, i) => (
            <div
              key={step.step}
              className={`flex items-center gap-3 px-4 py-3.5 ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              {step.status === "done" ? (
                <CheckCircle size={14} className="text-green-400 shrink-0" />
              ) : step.status === "pending" ? (
                <AlertTriangle size={14} className="text-yellow-400 shrink-0" />
              ) : (
                <Lock size={14} className="text-neutral-700 shrink-0" />
              )}
              <span
                className={`flex-1 text-[12px] ${
                  step.status === "done"
                    ? "text-neutral-400"
                    : step.status === "pending"
                    ? "text-yellow-300"
                    : "text-neutral-700"
                }`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {step.step}
              </span>
              <span
                className="text-neutral-700 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {step.time}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Code Diff ─────────────────────────────────────────────────────────────
  const CodeDiffScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="auth.service.ts" subtitle="SRC/AUTH/ · +12 -3" />

      <div className="px-4 pt-4 mb-3 flex items-center gap-2 flex-wrap">
        <span
          className="text-[10px] px-2 py-0.5 bg-green-500/10 text-green-400 rounded-lg"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          +12 adições
        </span>
        <span
          className="text-[10px] px-2 py-0.5 bg-red-500/10 text-red-400 rounded-lg"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          -3 remoções
        </span>
        <span
          className="text-[10px] px-2 py-0.5 bg-[#16162a] text-neutral-500 rounded-lg"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Linhas 278–298
        </span>
      </div>

      <div className="px-4 mb-4">
        <div className="bg-[#07070d] border border-white/[0.06] rounded-2xl overflow-hidden">
          <div className="flex items-center gap-2 px-4 py-2.5 border-b border-white/[0.06] bg-[#0a0a12]">
            <FileCode size={12} className="text-violet-400" />
            <span
              className="text-neutral-500 text-[10px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              src/auth/auth.service.ts
            </span>
          </div>
          <div className="overflow-x-auto" style={{ scrollbarWidth: "none" }}>
            <div
              className="text-[11px] leading-relaxed p-4 min-w-max space-y-0.5"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              {[
                { ln: "278", type: "ctx", code: "  async refreshSession(userId: string) {" },
                { ln: "279", type: "ctx", code: "    const user = await this.userRepo.findById(userId);" },
                { ln: "280", type: "ctx", code: "    if (!user) throw new NotFoundException();" },
                { ln: "", type: "ctx", code: "" },
                { ln: "282", type: "rem", code: "    const token = await this.refreshToken(user.id)" },
                { ln: "283", type: "rem", code: "    if (token.expired) throw new Error('expired')" },
                { ln: "284", type: "rem", code: "    return token" },
                { ln: "", type: "add", code: "    const token = await this.refreshToken(user.id, {" },
                { ln: "", type: "add", code: "      force: false, audience: user.role" },
                { ln: "", type: "add", code: "    })" },
                { ln: "", type: "add", code: "" },
                { ln: "", type: "add", code: "    if (!token || token.expiresAt < Date.now()) {" },
                { ln: "", type: "add", code: "      await this.clearSession(user.id)" },
                { ln: "", type: "add", code: "      throw new AuthException('TOKEN_EXPIRED', {" },
                { ln: "", type: "add", code: "        userId: user.id, reason: 'session_expired'" },
                { ln: "", type: "add", code: "      })" },
                { ln: "", type: "add", code: "    }" },
                { ln: "", type: "add", code: "    return token" },
                { ln: "285", type: "ctx", code: "  }" },
              ].map((row, i) => (
                <div
                  key={i}
                  className={`flex gap-3 px-1 py-0.5 rounded ${
                    row.type === "rem"
                      ? "bg-red-500/10"
                      : row.type === "add"
                      ? "bg-green-500/10"
                      : ""
                  }`}
                >
                  <span className="text-neutral-700 w-6 shrink-0 text-right text-[10px]">
                    {row.ln}
                  </span>
                  <span
                    className={`whitespace-pre ${
                      row.type === "rem"
                        ? "text-red-400"
                        : row.type === "add"
                        ? "text-green-400"
                        : "text-neutral-500"
                    }`}
                  >
                    {row.type === "rem" ? "- " : row.type === "add" ? "+ " : "  "}
                    {row.code}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="px-4 mb-6">
        <div className="bg-[#0f0f18] border border-violet-500/15 rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-2">
            <Bot size={13} className="text-violet-400" />
            <span
              className="text-violet-400 text-[10px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Agent Dev · explicação
            </span>
          </div>
          <p className="text-neutral-400 text-[12px] leading-relaxed">
            O bug era causado por falta de verificação de nulo no token retornado e pela comparação
            incorreta de expiração. A exceção genérica foi substituída por{" "}
            <code
              className="bg-white/5 px-1 rounded text-violet-300"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              AuthException
            </code>{" "}
            com contexto estruturado.
          </p>
        </div>
      </div>
    </div>
  );

  // ── Terminal ──────────────────────────────────────────────────────────────
  const TerminalScreen = () => (
    <div className="flex-1 flex flex-col overflow-hidden">
      <TopBar title="Terminal" subtitle="WORKSTATION-01 · BASH" />
      <div className="flex-1 bg-[#06060a] mx-4 my-4 rounded-2xl border border-white/[0.06] overflow-hidden flex flex-col">
        <div className="flex items-center gap-1.5 px-4 py-2.5 border-b border-white/[0.06] bg-[#0a0a12] shrink-0">
          <div className="w-2.5 h-2.5 rounded-full bg-red-500/60" />
          <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/60" />
          <div className="w-2.5 h-2.5 rounded-full bg-green-500/60" />
          <span
            className="text-neutral-700 text-[10px] ml-2"
            style={{ fontFamily: "'JetBrains Mono', monospace" }}
          >
            agent-dev@workstation-01
          </span>
        </div>
        <div
          className="flex-1 overflow-y-auto p-4 space-y-0.5"
          style={{ scrollbarWidth: "none", fontFamily: "'JetBrains Mono', monospace" }}
        >
          {[
            { text: `agent@ws-01:~/atlas$ git checkout -b fix/auth-bug-184`, type: "cmd" },
            { text: `Switched to a new branch 'fix/auth-bug-184'`, type: "out" },
            { text: `agent@ws-01:~/atlas$ grep -n "refreshToken" src/auth/auth.service.ts`, type: "cmd" },
            { text: `282:    const token = await this.refreshToken(user.id)`, type: "out" },
            { text: `283:    if (token.expired) throw new Error('expired')`, type: "out" },
            { text: `agent@ws-01:~/atlas$ npm test -- --testPathPattern=auth`, type: "cmd" },
            { text: ` RUNS  src/auth/__tests__/auth.service.spec.ts`, type: "info" },
            { text: ` PASS  src/auth/__tests__/auth.service.spec.ts (2.341s)`, type: "pass" },
            { text: `  ✓ should authenticate valid user (45ms)`, type: "pass_sm" },
            { text: `  ✓ should reject invalid credentials (12ms)`, type: "pass_sm" },
            { text: `  ✓ should handle expired token gracefully (28ms)`, type: "pass_sm" },
            { text: `  ✓ should clear session on token error (19ms)`, type: "pass_sm" },
            { text: `Tests: 8 passed, 8 total`, type: "info" },
            { text: `agent@ws-01:~/atlas$ `, type: "prompt" },
          ].map((line, i) => {
            const cls =
              line.type === "cmd"
                ? "text-neutral-300"
                : line.type === "pass"
                ? "text-green-400"
                : line.type === "pass_sm"
                ? "text-green-500/70"
                : line.type === "info"
                ? "text-neutral-500"
                : line.type === "prompt"
                ? "text-neutral-300"
                : "text-neutral-600";
            return (
              <p key={i} className={`text-[11px] leading-relaxed ${cls}`}>
                {line.text}
                {line.type === "prompt" && (
                  <span className="animate-pulse">▋</span>
                )}
              </p>
            );
          })}
        </div>
        <div className="px-4 py-3 border-t border-white/[0.06] flex items-center gap-3 shrink-0">
          <span
            className="text-neutral-700 text-[11px]"
            style={{ fontFamily: "'JetBrains Mono', monospace" }}
          >
            $
          </span>
          <input
            className="flex-1 bg-transparent text-neutral-300 text-[11px] outline-none placeholder:text-neutral-800"
            style={{ fontFamily: "'JetBrains Mono', monospace" }}
            placeholder="Digite um comando..."
          />
          <button>
            <Send size={14} className="text-violet-500" />
          </button>
        </div>
      </div>
    </div>
  );

  // ── Approval ──────────────────────────────────────────────────────────────
  const ApprovalScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Aprovação Necessária" subtitle="AGENT DEVOPS · STAGING" />

      <div className="px-4 pt-4 mb-5">
        <div className="bg-yellow-500/5 border border-yellow-500/20 rounded-2xl p-5">
          <div className="flex items-start gap-3 mb-4">
            <div className="w-11 h-11 rounded-2xl bg-yellow-500/15 flex items-center justify-center shrink-0">
              <AlertTriangle size={20} className="text-yellow-400" />
            </div>
            <div>
              <p className="text-white font-semibold text-[15px]">Deploy em staging</p>
              <p
                className="text-yellow-600 text-[11px] mt-0.5"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                Agent DevOps solicita aprovação
              </p>
            </div>
          </div>
          <p className="text-neutral-400 text-[13px] leading-relaxed">
            O Agent DevOps preparou o deploy do fix do bug #184 para o ambiente de staging. Todos os
            testes unitários passaram com sucesso.
          </p>
        </div>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Detalhes do Deploy
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { label: "Ambiente", value: "staging.atlas.dev" },
            { label: "Branch", value: "fix/auth-bug-184" },
            { label: "Commits", value: "3 novos commits" },
            { label: "Imagem", value: "atlas:1.4.2-staging.7" },
            { label: "Rollback", value: "Automático em falha" },
          ].map((item, i) => (
            <div
              key={item.label}
              className={`flex items-center px-4 py-3.5 ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              <span className="text-neutral-600 text-[11px] w-24 shrink-0">{item.label}</span>
              <span
                className="text-violet-300 text-[11px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {item.value}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Testes · 32/32 passaram
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4">
          <div className="flex items-center justify-between mb-3">
            <span className="text-neutral-400 text-[12px]">32 testes executados</span>
            <span
              className="text-green-400 text-[12px] font-semibold"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              100% OK
            </span>
          </div>
          <Bar value={100} color="green" />
          <div className="flex gap-4 mt-3">
            <div className="flex items-center gap-1.5">
              <CheckCircle size={12} className="text-green-400" />
              <span className="text-neutral-500 text-[11px]">32 passaram</span>
            </div>
            <div className="flex items-center gap-1.5">
              <XCircle size={12} className="text-neutral-700" />
              <span className="text-neutral-700 text-[11px]">0 falharam</span>
            </div>
          </div>
        </div>
      </div>

      {approvalDone ? (
        <div className="px-4 mb-6 space-y-3">
          <div
            className={`w-full py-4 rounded-2xl flex items-center justify-center gap-2 ${
              approvalDone === "approved"
                ? "bg-green-500/10 border border-green-500/25"
                : "bg-red-500/10 border border-red-500/20"
            }`}
          >
            {approvalDone === "approved" ? (
              <CheckCircle size={18} className="text-green-400" />
            ) : (
              <XCircle size={18} className="text-red-400" />
            )}
            <span
              className={`text-[14px] font-semibold ${
                approvalDone === "approved" ? "text-green-400" : "text-red-400"
              }`}
            >
              {approvalDone === "approved" ? "Deploy aprovado!" : "Deploy rejeitado"}
            </span>
          </div>
          {approvalDone === "approved" && (
            <button
              onClick={() => go("result")}
              className="w-full py-4 bg-violet-600 rounded-2xl text-white text-[14px] font-semibold"
            >
              Ver resultado →
            </button>
          )}
        </div>
      ) : (
        <div className="px-4 mb-6 space-y-3">
          <button
            onClick={() => setApprovalDone("approved")}
            className="w-full py-4 bg-green-500 rounded-2xl text-white text-[14px] font-semibold active:bg-green-600 transition-colors"
          >
            Aprovar Deploy
          </button>
          <button
            onClick={() => setApprovalDone("rejected")}
            className="w-full py-3.5 bg-[#0f0f18] border border-red-500/25 rounded-2xl text-red-400 text-[14px] font-semibold active:bg-red-500/5 transition-colors"
          >
            Rejeitar
          </button>
          <button
            onClick={() => go("agent-devops")}
            className="w-full py-3 text-neutral-600 text-[13px]"
          >
            Ver detalhes do Agent DevOps
          </button>
        </div>
      )}
    </div>
  );

  // ── Machines ──────────────────────────────────────────────────────────────
  const MachinesScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Máquinas" subtitle="4 REGISTRADAS · 3 ONLINE" />
      <div className="px-4 pt-4 space-y-3 pb-4">
        {[
          { name: "Workstation-01", role: "Dev Machine · Ubuntu 24.04", cpu: 45, ram: 62, disk: 38, status: "online" as StatusType, agents: 1 },
          { name: "Server-Atlas", role: "App Server · Docker · Node.js", cpu: 23, ram: 41, disk: 67, status: "online" as StatusType, agents: 1 },
          { name: "Server-CI", role: "CI/CD · GitHub Actions Runner", cpu: 71, ram: 55, disk: 22, status: "online" as StatusType, agents: 1 },
          { name: "Workstation-02", role: "Dev Machine · macOS 15", cpu: 0, ram: 0, disk: 0, status: "offline" as StatusType, agents: 0 },
        ].map((machine) => (
          <button
            key={machine.name}
            onClick={() => machine.status === "online" && go("machine-detail")}
            className={`w-full bg-[#0f0f18] border rounded-2xl p-4 text-left ${
              machine.status === "offline"
                ? "border-white/[0.04] opacity-40"
                : "border-white/[0.06] active:scale-[0.99] transition-transform"
            }`}
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
                  <Monitor
                    size={16}
                    className={machine.status === "online" ? "text-neutral-400" : "text-neutral-700"}
                  />
                </div>
                <div>
                  <p className="text-white text-[13px] font-semibold">{machine.name}</p>
                  <p
                    className="text-neutral-600 text-[10px]"
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    {machine.role}
                  </p>
                </div>
              </div>
              <div className="flex flex-col items-end gap-1">
                <div className="flex items-center gap-1.5">
                  <StatusDot status={machine.status} />
                  <span
                    className={`text-[10px] uppercase ${
                      machine.status === "online" ? "text-green-400" : "text-neutral-600"
                    }`}
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    {machine.status}
                  </span>
                </div>
                {machine.agents > 0 && (
                  <span
                    className="text-violet-400 text-[9px]"
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    {machine.agents} agente ativo
                  </span>
                )}
              </div>
            </div>
            {machine.status === "online" && (
              <div className="space-y-2">
                {[
                  { label: "CPU", value: machine.cpu, color: machine.cpu > 70 ? "red" : "green" as const },
                  { label: "RAM", value: machine.ram, color: "blue" as const },
                  { label: "Disk", value: machine.disk, color: "violet" as const },
                ].map(({ label, value, color }) => (
                  <div key={label} className="flex items-center gap-3">
                    <span
                      className="text-neutral-700 text-[9px] w-7"
                      style={{ fontFamily: "'JetBrains Mono', monospace" }}
                    >
                      {label}
                    </span>
                    <div className="flex-1 h-1 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          color === "green"
                            ? "bg-green-500"
                            : color === "red"
                            ? "bg-red-500"
                            : color === "blue"
                            ? "bg-blue-500"
                            : "bg-violet-500"
                        }`}
                        style={{ width: `${value}%` }}
                      />
                    </div>
                    <span
                      className="text-neutral-600 text-[9px] w-8 text-right"
                      style={{ fontFamily: "'JetBrains Mono', monospace" }}
                    >
                      {value}%
                    </span>
                  </div>
                ))}
              </div>
            )}
          </button>
        ))}
      </div>
    </div>
  );

  // ── Machine Detail ────────────────────────────────────────────────────────
  const MachineDetailScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Workstation-01" subtitle="ONLINE · UBUNTU 24.04 · 32GB RAM" />

      <div className="px-4 pt-4 mb-5">
        <div className="grid grid-cols-3 gap-2">
          {[
            { label: "CPU", value: "45%", color: "text-green-400", bg: "bg-green-500/8" },
            { label: "RAM", value: "62%", color: "text-blue-400", bg: "bg-blue-500/8" },
            { label: "Disk", value: "38%", color: "text-violet-400", bg: "bg-violet-500/8" },
          ].map((stat) => (
            <div
              key={stat.label}
              className={`${stat.bg} border border-white/[0.06] rounded-2xl p-3 flex flex-col items-center gap-1`}
            >
              <p
                className="text-neutral-600 text-[10px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {stat.label}
              </p>
              <p
                className={`${stat.color} text-2xl font-bold`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {stat.value}
              </p>
            </div>
          ))}
        </div>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Agente Ativo
        </p>
        <button
          onClick={() => go("agent-dev")}
          className="w-full bg-[#0f0f18] border border-violet-500/20 rounded-2xl p-4 flex items-center gap-3"
        >
          <div className="w-9 h-9 rounded-xl bg-violet-600/20 flex items-center justify-center">
            <Bot size={16} className="text-violet-400" />
          </div>
          <div className="flex-1 text-left">
            <p className="text-white text-[13px] font-medium">Agent Dev</p>
            <p
              className="text-neutral-600 text-[11px]"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Bug #184 · 68%
            </p>
          </div>
          <ChevronRight size={14} className="text-neutral-700" />
        </button>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Acesso Rápido
        </p>
        <div className="grid grid-cols-3 gap-2">
          {[
            { label: "Terminal", Icon: Terminal, color: "text-green-400", action: () => go("terminal") },
            { label: "SSH", Icon: Shield, color: "text-blue-400", action: () => {} },
            { label: "Arquivos", Icon: FolderOpen, color: "text-yellow-400", action: () => go("files") },
            { label: "Docker", Icon: Box, color: "text-cyan-400", action: () => {} },
            { label: "Logs", Icon: Activity, color: "text-orange-400", action: () => {} },
            { label: "Tela", Icon: Monitor, color: "text-violet-400", action: () => {} },
          ].map(({ label, Icon, color, action }) => (
            <button
              key={label}
              onClick={action}
              className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl py-4 flex flex-col items-center gap-2 active:bg-white/[0.03] transition-colors"
            >
              <Icon size={18} className={color} />
              <span className="text-neutral-500 text-[11px]">{label}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="px-4 mb-6">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Processos
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { name: "node", pid: "12847", cpu: "23.4", mem: "412 MB" },
            { name: "typescript-ls", pid: "12901", cpu: "8.1", mem: "198 MB" },
            { name: "docker", pid: "11203", cpu: "2.3", mem: "89 MB" },
            { name: "agent-runtime", pid: "13421", cpu: "1.1", mem: "67 MB" },
          ].map((proc, i) => (
            <div
              key={proc.name}
              className={`flex items-center px-4 py-3 ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              <span className="text-neutral-400 text-[11px] flex-1">{proc.name}</span>
              <span className="text-neutral-700 text-[9px] w-12 text-right">{proc.pid}</span>
              <span className="text-green-400 text-[10px] w-12 text-right">{proc.cpu}%</span>
              <span className="text-blue-400 text-[10px] w-16 text-right">{proc.mem}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  // ── New Task ──────────────────────────────────────────────────────────────
  const NewTaskScreen = () => (
    <div className="flex-1 flex flex-col overflow-hidden">
      <TopBar title="Nova Tarefa" subtitle="PROJETO ATLAS" />

      <div className="flex-1 overflow-y-auto px-4 pt-4" style={{ scrollbarWidth: "none" }}>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden mb-5">
          <div className="p-4">
            <p
              className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
              style={{ fontFamily: "'JetBrains Mono', monospace" }}
            >
              Descreva a tarefa
            </p>
            <textarea
              className="w-full bg-transparent text-white text-[14px] leading-relaxed resize-none outline-none placeholder:text-neutral-700 min-h-[72px]"
              placeholder="Ex: Investigue e corrija o bug #184 no módulo de auth..."
              value={newTaskText}
              onChange={(e) => setNewTaskText(e.target.value)}
            />
          </div>
          {newTaskText.length > 0 && (
            <div className="border-t border-white/[0.06] px-4 py-3.5">
              <div className="flex items-center gap-2 mb-2.5">
                <Zap size={12} className="text-violet-400" />
                <span className="text-neutral-600 text-[11px]">Agentes sugeridos pelo orquestrador</span>
              </div>
              <div className="flex gap-2 flex-wrap">
                {["Agent Dev", "Agent QA", "Agent DevOps"].map((a) => (
                  <span
                    key={a}
                    className="text-[11px] px-2.5 py-1 bg-violet-600/15 border border-violet-500/20 text-violet-300 rounded-lg"
                    style={{ fontFamily: "'JetBrains Mono', monospace" }}
                  >
                    {a}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Sugestões
        </p>
        <div className="space-y-2 pb-4">
          {[
            "Investigar e corrigir bug #184 no módulo de autenticação",
            "Adicionar rate limiting na API de login com Redis",
            "Revisar e otimizar queries lentas no dashboard de analytics",
            "Criar testes E2E para o fluxo completo de checkout",
          ].map((suggestion) => (
            <button
              key={suggestion}
              onClick={() => setNewTaskText(suggestion)}
              className="w-full bg-[#0f0f18] border border-white/[0.06] rounded-2xl px-4 py-3.5 flex items-center gap-3 text-left active:bg-white/[0.02]"
            >
              <Zap size={13} className="text-neutral-700 shrink-0" />
              <span className="text-neutral-400 text-[13px] leading-snug">{suggestion}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="px-4 pb-4 pt-3 border-t border-white/[0.06] shrink-0">
        <button
          onClick={() => {
            if (newTaskText.trim()) go("task-detail");
          }}
          className={`w-full py-4 rounded-2xl text-[14px] font-semibold transition-all ${
            newTaskText.trim()
              ? "bg-violet-600 text-white"
              : "bg-[#16162a] text-neutral-700"
          }`}
        >
          {newTaskText.trim() ? "Criar e iniciar tarefa →" : "Digite uma tarefa para continuar"}
        </button>
      </div>
    </div>
  );

  // ── Files ─────────────────────────────────────────────────────────────────
  const FilesScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Arquivos" subtitle="WORKSTATION-01 · ~/ATLAS/SRC/AUTH" />
      <div className="px-4 pt-4">
        <div
          className="flex items-center gap-1 mb-4 overflow-x-auto pb-1"
          style={{ scrollbarWidth: "none", fontFamily: "'JetBrains Mono', monospace" }}
        >
          {["~", "atlas", "src", "auth"].map((crumb, i) => (
            <div key={crumb} className="flex items-center gap-1 shrink-0">
              {i > 0 && <span className="text-neutral-700">/</span>}
              <span className={`text-[11px] ${i === 3 ? "text-violet-400" : "text-neutral-600"}`}>
                {crumb}
              </span>
            </div>
          ))}
        </div>

        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden mb-4">
          {[
            { name: "__tests__", type: "dir", size: "—", modified: "2m atrás", hot: false },
            { name: "auth.controller.ts", type: "file", size: "4.2 KB", modified: "1h atrás", hot: false },
            { name: "auth.guards.ts", type: "file", size: "2.1 KB", modified: "3h atrás", hot: false },
            { name: "auth.module.ts", type: "file", size: "1.8 KB", modified: "1d atrás", hot: false },
            { name: "auth.service.ts", type: "file", size: "8.7 KB", modified: "2m atrás", hot: true },
            { name: "auth.types.ts", type: "file", size: "3.4 KB", modified: "5m atrás", hot: true },
          ].map((item, i) => (
            <button
              key={item.name}
              onClick={() => (item.type === "file" ? go("code-diff") : null)}
              className={`w-full flex items-center gap-3 px-4 py-3.5 active:bg-white/[0.02] ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              {item.type === "dir" ? (
                <FolderOpen size={14} className="text-yellow-500/70 shrink-0" />
              ) : (
                <FileCode
                  size={14}
                  className={`shrink-0 ${item.hot ? "text-violet-400" : "text-neutral-700"}`}
                />
              )}
              <span
                className={`flex-1 text-left text-[12px] ${item.hot ? "text-white" : "text-neutral-400"}`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {item.name}
              </span>
              <span
                className="text-neutral-700 text-[10px] mr-2"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {item.size}
              </span>
              <span
                className={`text-[10px] ${item.hot ? "text-violet-400" : "text-neutral-700"}`}
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {item.modified}
              </span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Result ────────────────────────────────────────────────────────────────
  const ResultScreen = () => (
    <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
      <TopBar title="Tarefa Concluída" subtitle="BUG #184 · 1H 12MIN" />

      <div className="px-4 pt-4 mb-5">
        <div className="bg-green-500/5 border border-green-500/15 rounded-2xl p-6 flex flex-col items-center text-center">
          <div className="w-16 h-16 rounded-full bg-green-500/15 border border-green-500/20 flex items-center justify-center mb-4">
            <CheckCircle size={30} className="text-green-400" />
          </div>
          <p className="text-white font-bold text-[18px] mb-2">Bug #184 Resolvido</p>
          <p className="text-neutral-500 text-[13px] leading-relaxed">
            Crash no módulo de autenticação identificado e corrigido. Deploy em staging realizado com
            sucesso.
          </p>
        </div>
      </div>

      <div className="px-4 mb-5">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Resumo
        </p>
        <div className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl overflow-hidden">
          {[
            { label: "Causa raiz", value: "Token null não tratado na linha 284", Icon: Bug },
            { label: "Arquivos", value: "3 modificados", Icon: FileCode },
            { label: "Testes", value: "32/32 passaram", Icon: CheckCircle },
            { label: "Deploy", value: "staging.atlas.dev ✓", Icon: Globe },
            { label: "Tempo total", value: "1h 12min · 3 agentes", Icon: Clock },
          ].map((item, i) => (
            <div
              key={item.label}
              className={`flex items-center gap-3 px-4 py-3.5 ${i > 0 ? "border-t border-white/[0.04]" : ""}`}
            >
              <item.Icon size={14} className="text-neutral-700 shrink-0" />
              <span className="text-neutral-600 text-[11px] w-24 shrink-0">{item.label}</span>
              <span
                className="text-neutral-300 text-[11px]"
                style={{ fontFamily: "'JetBrains Mono', monospace" }}
              >
                {item.value}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="px-4 mb-6">
        <p
          className="text-neutral-600 text-[10px] uppercase tracking-widest mb-3"
          style={{ fontFamily: "'JetBrains Mono', monospace" }}
        >
          Contribuições dos Agentes
        </p>
        <div className="space-y-2.5">
          {[
            {
              name: "Agent Dev",
              contribution: "Identificou bug, aplicou patch, escreveu testes unitários",
              time: "48min",
              color: "text-violet-400",
            },
            {
              name: "Agent QA",
              contribution: "Executou suite completa de 32 testes · todos passaram",
              time: "18min",
              color: "text-blue-400",
            },
            {
              name: "Agent DevOps",
              contribution: "Build, imagem Docker e deploy em staging",
              time: "6min",
              color: "text-yellow-400",
            },
          ].map((agent) => (
            <div key={agent.name} className="bg-[#0f0f18] border border-white/[0.06] rounded-2xl p-4">
              <div className="flex items-center justify-between mb-1.5">
                <p className={`text-[13px] font-semibold ${agent.color}`}>{agent.name}</p>
                <span
                  className="text-neutral-700 text-[10px]"
                  style={{ fontFamily: "'JetBrains Mono', monospace" }}
                >
                  {agent.time}
                </span>
              </div>
              <p className="text-neutral-500 text-[12px] leading-snug">{agent.contribution}</p>
            </div>
          ))}
        </div>

        <button
          onClick={() => {
            setHistory([]);
            setApprovalDone(null);
            setScreen("home");
          }}
          className="w-full mt-5 py-4 bg-violet-600 rounded-2xl text-white text-[14px] font-semibold"
        >
          Voltar ao início
        </button>
      </div>
    </div>
  );

  // ── Router ────────────────────────────────────────────────────────────────
  const renderScreen = () => {
    switch (screen) {
      case "home": return <HomeScreen />;
      case "tasks": return <TaskListScreen />;
      case "task-detail": return <TaskDetailScreen />;
      case "agent-dev": return <AgentDevScreen />;
      case "agent-qa": return <AgentQAScreen />;
      case "agent-devops": return <AgentDevOpsScreen />;
      case "code-diff": return <CodeDiffScreen />;
      case "terminal": return <TerminalScreen />;
      case "approval": return <ApprovalScreen />;
      case "machines": return <MachinesScreen />;
      case "machine-detail": return <MachineDetailScreen />;
      case "new-task": return <NewTaskScreen />;
      case "files": return <FilesScreen />;
      case "result": return <ResultScreen />;
      default: return <HomeScreen />;
    }
  };

  // ── Root ──────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-[#040407] flex items-center justify-center p-4 md:p-8">
      {/* Phone chrome */}
      <div
        className="relative w-[390px] h-[844px] bg-[#09090e] rounded-[48px] overflow-hidden shadow-2xl shadow-black flex flex-col"
        style={{ border: "1.5px solid rgba(255,255,255,0.09)" }}
      >
        {/* Status bar */}
        <div className="flex items-center justify-between px-8 pt-4 pb-1 shrink-0">
          <span
            className="text-white text-[12px] font-semibold"
            style={{ fontFamily: "'Inter', sans-serif" }}
          >
            9:41
          </span>
          <div className="flex items-center gap-1.5">
            <Wifi size={13} className="text-white" />
            <div className="flex items-end gap-px">
              {[3, 4, 5, 6].map((h) => (
                <div
                  key={h}
                  className="w-1 bg-white rounded-sm"
                  style={{ height: `${h}px` }}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Content area */}
        <div className="flex-1 flex flex-col overflow-hidden">{renderScreen()}</div>

        {/* Bottom nav */}
        <NavBar />

        {/* Plus overlay */}
        {plusOpen && <PlusMenu />}
      </div>
    </div>
  );
}
