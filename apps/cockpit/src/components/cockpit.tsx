// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Platform investigation navigation
"use client";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  Activity,
  ArrowDownToLine,
  ArrowRight,
  ArrowUpRight,
  Check,
  CheckCheck,
  ChevronRight,
  CircleCheck,
  Circle,
  FileText,
  Fingerprint,
  GitBranch,
  Layers3,
  MapPin,
  Plus,
  Radio,
  Search,
  ShieldCheck,
  Waypoints,
} from "lucide-react";
import {
  actionLabels,
  nextStates,
  stateLabels,
  type Audit,
  type Identity,
  type Intelligence,
  type Mission,
  type Task,
  type TaskState,
  type MissionTab as Tab,
} from "@/lib/contracts";
import { date, pretty } from "@/lib/presentation";
import { api } from "@/lib/api";
import {
  Badge,
  Spinner,
  Empty,
  Dialog,
  Form,
  Field,
  SectionHeading,
} from "./primitives";
import { Situation } from "./situation";
import { Options } from "./options";
import { EvidenceDetail } from "./evidence-detail";
import { IntelligenceView } from "./intelligence";
import { HistoryView } from "./history";
import { Investigations } from "./investigations";
type View = "desk" | "investigations" | "intelligence" | "activity";
type Modal =
  | { type: "create" }
  | { type: "source" }
  | { type: "evidence"; id: string }
  | { type: "task"; task: Task; target: TaskState }
  | null;
const tabs: Tab[] = [
  "Situation",
  "Evidence",
  "Options",
  "Operations",
  "Outcome",
];
export function Cockpit() {
  const [missions, setMissions] = useState<Mission[]>([]),
    [identity, setIdentity] = useState<Identity | null>(null),
    [selected, setSelected] = useState("VGL-0041");
  const [view, setView] = useState<View>("investigations"),
    [tab, setTab] = useState<Tab>("Situation"),
    [query, setQuery] = useState(""),
    [filter, setFilter] = useState("All missions");
  const [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [toast, setToast] = useState(""),
    [modal, setModal] = useState<Modal>(null);
  const [audit, setAudit] = useState<Audit | null>(null),
    [intelligence, setIntelligence] = useState<Intelligence | null>(null),
    [auxError, setAuxError] = useState("");
  const search = useRef<HTMLInputElement>(null),
    pending = useRef<{ signature: string; key: string } | null>(null);
  const loadSequence = useRef(0);
  const mission = missions.find((item) => item.id === selected) || missions[0];
  const write =
    !!identity && !identity.read_only && mission?.status !== "COMPLETED";
  const current =
    !!mission?.plan &&
    mission.plan.evidence_version === mission.evidence_version;
  const load = useCallback(async () => {
    const sequence = ++loadSequence.current;
    const result = await api<{ identity: Identity; missions: Mission[] }>(
      "missions",
    );
    if (sequence !== loadSequence.current) return;
    setIdentity(result.identity);
    setMissions(result.missions);
    setSelected((id) =>
      result.missions.some((m) => m.id === id)
        ? id
        : result.missions[0]?.id || "",
    );
  }, []);
  useEffect(() => {
    load()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [load]);
  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        search.current?.focus();
      }
    };
    window.addEventListener("keydown", key);
    return () => window.removeEventListener("keydown", key);
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(""), 4500);
    return () => clearTimeout(timer);
  }, [toast]);
  useEffect(() => {
    let cancelled = false;
    setAuxError("");
    if (view === "activity" && mission) {
      setAudit(null);
      api<Audit>(`missions/${mission.id}/audit`)
        .then((data) => {
          if (!cancelled) setAudit(data);
        })
        .catch((e) => {
          if (!cancelled) setAuxError(e.message);
        });
    }
    if (view === "intelligence") {
      api<Intelligence>("intelligence/status")
        .then((data) => {
          if (!cancelled) setIntelligence(data);
        })
        .catch((e) => {
          if (!cancelled) setAuxError(e.message);
        });
    }
    return () => {
      cancelled = true;
    };
  }, [view, mission]);
  async function switchWorkspace(actor: string) {
    if (busy) return;
    // Invalidate an earlier refresh before clearing the previous workspace.
    ++loadSequence.current;
    setBusy(true);
    setError("");
    setModal(null);
    setAudit(null);
    setIntelligence(null);
    setMissions([]);
    setView("desk");
    setIdentity(null);
    pending.current = null;
    try {
      await api("session", { actor });
      await load();
      setTab("Situation");
      setView("investigations");
      setQuery("");
      setFilter("All missions");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function execute(payload: Record<string, unknown>, create = false) {
    if (busy) throw new Error("Wait for the current operation to finish.");
    const path = create ? "missions" : `missions/${mission!.id}/commands`;
    const body = create
      ? payload
      : { ...payload, expected_version: mission!.version };
    const signature = JSON.stringify({ path, body });
    if (pending.current?.signature !== signature)
      pending.current = { signature, key: crypto.randomUUID() };
    setBusy(true);
    setError("");
    try {
      const updated = await api<Mission>(path, body, pending.current.key);
      pending.current = null;
      setMissions((items) => [
        updated,
        ...items.filter((item) => item.id !== updated.id),
      ]);
      setSelected(updated.id);
      setToast(create ? "Mission opened." : "Mission record updated.");
    } finally {
      setBusy(false);
    }
  }
  async function generate() {
    try {
      await execute({ type: "generate_plan" });
      setTab("Options");
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function refresh() {
    setError("");
    setLoading(true);
    try {
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }
  const visible = missions.filter(
    (m) =>
      (filter === "All missions" ||
        m.status === (filter === "Active" ? "ACTIVE" : "COMPLETED")) &&
      `${m.title} ${m.id} ${m.location} ${m.domain}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const done =
    mission?.tasks.filter((t) => t.state === "CONFIRMED").length || 0;
  return (
    <div className="app-shell">
      <a href="#main" className="skip-link">
        Skip to mission workspace
      </a>
      <aside className="sidebar">
        <Link className="brand" href="/" aria-label="Vigil home">
          <span className="brand-mark">
            <span />
            <span />
          </span>
          vigil<span className="brand-period">.</span>
        </Link>
        <div className="sidebar-caption">INTELLIGENCE INTO OPERATIONS</div>
        <div className="workspace-label">
          <span className="connection-dot" />
          <span>
            South Africa
            <span className="small-block">CONNECTED WORKSPACE / 02</span>
          </span>
        </div>
        <nav aria-label="Primary navigation">
          <button
            className={
              view === "investigations" ? "nav-item selected" : "nav-item"
            }
            onClick={() => setView("investigations")}
          >
            <GitBranch size={18} /> Investigations
          </button>
          <button
            className={view === "desk" ? "nav-item selected" : "nav-item"}
            onClick={() => setView("desk")}
          >
            <Layers3 size={18} />
            Mission desk
            <span className="nav-count">
              {missions.length.toString().padStart(2, "0")}
            </span>
          </button>
          <button
            className={
              view === "intelligence" ? "nav-item selected" : "nav-item"
            }
            onClick={() => setView("intelligence")}
          >
            <Waypoints size={18} />
            AI operations
          </button>
          <button
            className={view === "activity" ? "nav-item selected" : "nav-item"}
            onClick={() => setView("activity")}
          >
            <Activity size={18} />
            Decision history
          </button>
        </nav>
        <div className="sidebar-note">
          <span className="eyebrow">ONE CONNECTED PLATFORM</span>
          <p>
            Trace the evidence.
            <br />
            Connect the operational picture.
            <br />
            <em>Stay until it is done.</em>
          </p>
          <div className="mini-path">
            <Circle size={8} />
            <span />
            <Circle size={8} />
            <span />
            <CircleCheck size={13} />
          </div>
        </div>
        <div className="identity">
          <span className="avatar">
            {identity?.read_only
              ? "IR"
              : identity?.actor === "protection"
                ? "PP"
                : "OC"}
          </span>
          <div>
            <strong>{identity?.name || "Connecting…"}</strong>
            <span>{identity?.organisation || "Local mission services"}</span>
          </div>
        </div>
        <label className="workspace-switch">
          <span>Demonstration workspace</span>
          <select
            aria-label="Demonstration workspace"
            value={identity?.actor || "operations"}
            disabled={busy}
            onChange={(e) => switchWorkspace(e.target.value)}
          >
            <option value="operations">District operations</option>
            <option value="protection">Protection services</option>
            <option value="reviewer">Independent review · read only</option>
          </select>
        </label>
        <div className="sidebar-bottom">
          <span className="connection-dot" />
          LOCAL ENVIRONMENT <span>v0.2</span>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            Workspace
            <ChevronRight size={12} />
            <strong>
              {view === "desk"
                ? "Mission desk"
                : view === "investigations"
                  ? "Investigations"
                  : view === "intelligence"
                    ? "AI operations"
                    : "Decision history"}
            </strong>
          </div>
          <div className="topbar-right">
            <span className="demo-label">
              <span />
              SYNTHETIC SCENARIOS
            </span>
            <span className="region">
              ZA <span>·</span> SAST
            </span>
          </div>
        </header>
        <div className="demonstration-strip">
          <ShieldCheck size={14} />
          <span>
            A working demonstration with fictional records. Handoffs are
            recorded manually; no emergency services are contacted.
          </span>
        </div>
        <main id="main">
          <div className="page-heading">
            <div>
              <div className="eyebrow">
                {view === "investigations"
                  ? "VIGIL / INTELLIGENCE WORKSPACE"
                  : identity?.actor === "protection"
                    ? "CARE, WITH CONTINUITY"
                    : "FROM SIGNAL TO OUTCOME"}
              </div>
              <h1>
                {view === "desk"
                  ? "The mission desk."
                  : view === "investigations"
                    ? "See the connections."
                    : view === "intelligence"
                      ? "Intelligence, with evidence."
                      : "Every decision leaves a trace."}
              </h1>
              <p>
                {view === "desk"
                  ? "A shared picture. A clear next step. A result that holds up."
                  : view === "investigations"
                    ? "Evidence, entities, and events. Across the work that matters."
                    : view === "intelligence"
                      ? "Specialist reasoning, bounded tools, and visible uncertainty."
                      : "Follow the record from first signal to confirmed outcome."}
              </p>
            </div>
            {view === "desk" && (
              <button
                className="button primary"
                onClick={() => setModal({ type: "create" })}
                disabled={busy || !identity || identity.read_only}
              >
                <Plus size={17} />
                Open a mission
              </button>
            )}
          </div>
          {error && (
            <div role="alert" className="notice danger page-error">
              <span>{error}</span>
              <button className="text-button" onClick={refresh}>
                Refresh records <ArrowRight size={14} />
              </button>
            </div>
          )}
          {loading && !missions.length ? (
            <div className="loading">
              <Spinner /> Connecting to the mission services…
            </div>
          ) : view === "investigations" && identity ? (
            <Investigations
              key={identity.actor}
              identity={identity}
              missions={missions}
              onBusy={setBusy}
              refreshMissions={load}
              onMissionChange={(updated) =>
                setMissions((items) => [
                  updated,
                  ...items.filter((item) => item.id !== updated.id),
                ])
              }
              openMission={(id) => {
                setSelected(id);
                setTab("Evidence");
                setView("desk");
              }}
            />
          ) : view === "intelligence" ? (
            <IntelligenceView intelligence={intelligence} error={auxError} />
          ) : view === "activity" ? (
            <HistoryView
              audit={audit}
              error={auxError}
              mission={mission}
              missions={missions}
              select={setSelected}
            />
          ) : (
            <>
              <section className="desk-toolbar" aria-label="Mission filters">
                <div className="filter-tabs">
                  {["All missions", "Active", "Completed"].map((name) => (
                    <button
                      key={name}
                      onClick={() => setFilter(name)}
                      className={filter === name ? "active" : ""}
                    >
                      {name}
                      <span>
                        {name === "All missions"
                          ? missions.length
                          : missions.filter(
                              (m) =>
                                m.status ===
                                (name === "Active" ? "ACTIVE" : "COMPLETED"),
                            ).length}
                      </span>
                    </button>
                  ))}
                </div>
                <label className="search">
                  <Search size={16} />
                  <input
                    ref={search}
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Find a mission"
                    aria-label="Find a mission"
                  />
                  <kbd>Ctrl K</kbd>
                </label>
              </section>
              <div className="mission-layout">
                <aside className="mission-list" aria-label="Missions">
                  <div className="list-heading">
                    <span>YOUR MISSION QUEUE</span>
                    <span>{visible.length.toString().padStart(2, "0")}</span>
                  </div>
                  {visible.map((item) => (
                    <button
                      key={item.id}
                      className={`mission-card ${mission?.id === item.id ? "active" : ""}`}
                      onClick={() => {
                        setSelected(item.id);
                        setTab("Situation");
                        setError("");
                        pending.current = null;
                      }}
                      disabled={busy}
                    >
                      <div className="card-meta">
                        <span>{item.id}</span>
                        <span
                          className={`priority-dot ${item.priority.toLowerCase()}`}
                        />
                        {pretty(item.priority)}
                      </div>
                      <h3>{item.title}</h3>
                      <div className="card-place">
                        <MapPin size={12} />
                        {item.location}
                      </div>
                      <div className="card-footer">
                        <Badge
                          tone={item.domain === "PROTECT" ? "purple" : "sage"}
                        >
                          {pretty(item.domain)}
                        </Badge>
                        <span>
                          {item.status === "COMPLETED" ? (
                            <>
                              <CheckCheck size={13} />
                              Complete
                            </>
                          ) : (
                            <>
                              {item.evidence.length} sources
                              <ArrowUpRight size={13} />
                            </>
                          )}
                        </span>
                      </div>
                    </button>
                  ))}
                  {!visible.length && (
                    <div className="no-results">
                      No missions match this view.
                      <button
                        className="text-button"
                        onClick={() => {
                          setQuery("");
                          setFilter("All missions");
                        }}
                      >
                        Clear filters
                      </button>
                    </div>
                  )}
                  <div className="queue-footnote">
                    <Radio size={15} />
                    <span>
                      Each mission stays open until its outcome is accounted
                      for.
                    </span>
                  </div>
                </aside>
                {mission ? (
                  <section
                    className="mission-detail"
                    aria-label="Selected mission"
                  >
                    <div className="mission-heading">
                      <div className="mission-eyebrow">
                        <span className="eyebrow">
                          {mission.id} <span className="sep">/</span>{" "}
                          {pretty(mission.domain)}
                        </span>
                        <Badge
                          tone={
                            mission.status === "COMPLETED" ? "sage" : "orange"
                          }
                        >
                          {mission.status === "COMPLETED"
                            ? "Outcome recorded"
                            : "Mission active"}
                        </Badge>
                      </div>
                      <h2>{mission.title}</h2>
                      <div className="mission-meta">
                        <span>
                          <MapPin size={13} />
                          {mission.location}
                        </span>
                        <span>
                          <span className="tiny-avatar">
                            {mission.owner
                              .split(" ")
                              .map((s) => s[0])
                              .slice(0, 2)
                              .join("")}
                          </span>
                          {mission.owner}
                        </span>
                        <span>Revision {mission.version}</span>
                      </div>
                    </div>
                    <nav className="mission-tabs" aria-label="Mission views">
                      {tabs.map((name, i) => (
                        <button
                          key={name}
                          onClick={() => setTab(name)}
                          aria-current={tab === name ? "page" : undefined}
                          className={tab === name ? "active" : ""}
                        >
                          <span>0{i + 1}</span>
                          {name}
                          {name === "Evidence" && (
                            <b>{mission.evidence.length}</b>
                          )}
                          {name === "Options" && mission.plan && !current && (
                            <i className="stale-dot" title="Plan is stale" />
                          )}
                        </button>
                      ))}
                    </nav>
                    <div className="mission-body">
                      {mission.plan && !current && (
                        <div className="notice warning">
                          <GitBranch size={17} />
                          <span>
                            The evidence changed. This plan needs to be
                            refreshed before further authorisation or closure.
                          </span>
                          <button
                            className="text-button"
                            onClick={generate}
                            disabled={!write || busy}
                          >
                            Refresh plan
                          </button>
                        </div>
                      )}
                      {identity?.read_only && (
                        <div className="notice">
                          <ShieldCheck size={16} /> Independent review: this
                          identity can read and export, but cannot change
                          mission records.
                        </div>
                      )}
                      {tab === "Situation" && (
                        <Situation
                          mission={mission}
                          current={current}
                          setTab={setTab}
                          generate={generate}
                          busy={busy}
                          write={write}
                        />
                      )}
                      {tab === "Evidence" && (
                        <>
                          <SectionHeading
                            label="THE SOURCE REGISTER"
                            title="Keep the facts connected."
                            action={
                              <button
                                className="button secondary small"
                                disabled={!write || busy}
                                onClick={() => setModal({ type: "source" })}
                              >
                                <Plus size={14} />
                                Add source
                              </button>
                            }
                          />
                          <p className="section-intro">
                            Originals are preserved. Assessments can change.
                            Each change keeps its author and reason.
                          </p>
                          <div className="evidence-list">
                            {mission.evidence.map((e, i) => (
                              <button
                                className="evidence-row"
                                key={e.id}
                                onClick={() =>
                                  setModal({ type: "evidence", id: e.id })
                                }
                              >
                                <span className="source-icon">
                                  <FileText size={18} />
                                  <small>
                                    {(i + 1).toString().padStart(2, "0")}
                                  </small>
                                </span>
                                <span className="source-description">
                                  <span className="source-title">
                                    {e.title}
                                  </span>
                                  <span>{e.statement}</span>
                                  <small>
                                    {e.source} <span>·</span>{" "}
                                    {date(e.recorded_at)}
                                  </small>
                                </span>
                                <span className="source-status">
                                  <Badge
                                    tone={
                                      e.status === "SUPPORTED"
                                        ? "sage"
                                        : e.status === "DISPUTED"
                                          ? "orange"
                                          : "neutral"
                                    }
                                  >
                                    {pretty(e.status)}
                                  </Badge>
                                  <ArrowUpRight size={16} />
                                </span>
                              </button>
                            ))}
                          </div>
                          {!mission.evidence.length && (
                            <Empty title="Start with a source.">
                              Add the first observation or document. Analysis
                              remains unavailable until there is evidence to
                              inspect.
                            </Empty>
                          )}
                          <div className="integrity-note">
                            <Fingerprint size={17} />
                            <span>
                              SHA-256 fingerprints identify original text. They
                              detect changes; they do not prove a source is
                              truthful.
                            </span>
                          </div>
                        </>
                      )}
                      {tab === "Options" && (
                        <Options
                          mission={mission}
                          generate={generate}
                          busy={busy}
                          write={write}
                          source={(id) => setModal({ type: "evidence", id })}
                        />
                      )}
                      {tab === "Operations" && (
                        <>
                          <SectionHeading
                            label="ACCOUNTABLE HANDOFFS"
                            title="Sending is only the beginning."
                          />
                          <p className="section-intro">
                            Every transition requires a recorded receipt. This
                            demo does not send messages or dispatch a responder.
                          </p>
                          <div className="task-list">
                            {mission.tasks.map((task, i) => (
                              <article className="task" key={task.id}>
                                <div className="task-number">
                                  {task.state === "CONFIRMED" ? (
                                    <Check size={18} />
                                  ) : (
                                    String(i + 1).padStart(2, "0")
                                  )}
                                </div>
                                <div className="task-content">
                                  <div className="task-top">
                                    <h3>{task.title}</h3>
                                    <Badge
                                      tone={
                                        task.state === "CONFIRMED"
                                          ? "sage"
                                          : task.state === "DECLINED" ||
                                              task.state === "OUTCOME_UNKNOWN"
                                            ? "orange"
                                            : "neutral"
                                      }
                                    >
                                      {stateLabels[task.state]}
                                    </Badge>
                                  </div>
                                  <p className="task-owner">
                                    Accountable owner{" "}
                                    <strong>{task.owner}</strong>
                                  </p>
                                  <div className="task-stages">
                                    {[
                                      "PROPOSED",
                                      "AUTHORISED",
                                      "SENT",
                                      "ACCEPTED",
                                      "IN_PROGRESS",
                                      "CONFIRMED",
                                    ].map((s, index) => (
                                      <span
                                        key={s}
                                        className={
                                          index <=
                                          [
                                            "PROPOSED",
                                            "AUTHORISED",
                                            "SENT",
                                            "ACCEPTED",
                                            "IN_PROGRESS",
                                            "CONFIRMED",
                                          ].indexOf(task.state)
                                            ? "filled"
                                            : ""
                                        }
                                        title={pretty(s)}
                                      />
                                    ))}
                                  </div>
                                  {task.receipt && (
                                    <div className="receipt">
                                      <CheckCheck size={14} />
                                      <div>
                                        {task.receipt}
                                        <small>
                                          Recorded by {task.recorded_by} ·{" "}
                                          {date(task.updated_at)}
                                        </small>
                                      </div>
                                    </div>
                                  )}
                                  <div className="task-actions">
                                    {nextStates[task.state].map((target, j) => (
                                      <button
                                        className={`button ${j === 0 ? "secondary" : "quiet"} small`}
                                        key={target}
                                        disabled={
                                          !write ||
                                          busy ||
                                          (!current &&
                                            ["AUTHORISED", "SENT"].includes(
                                              target,
                                            ))
                                        }
                                        onClick={() =>
                                          setModal({
                                            type: "task",
                                            task,
                                            target,
                                          })
                                        }
                                      >
                                        {actionLabels[target]}
                                        {j === 0 && <ArrowRight size={13} />}
                                      </button>
                                    ))}
                                  </div>
                                </div>
                              </article>
                            ))}
                          </div>
                          {!current && (
                            <div className="notice warning">
                              Prepare a current plan in Options before
                              authorising or sending work.
                            </div>
                          )}
                        </>
                      )}
                      {tab === "Outcome" && (
                        <>
                          <SectionHeading
                            label="CLOSE THE LOOP"
                            title={
                              mission.outcome
                                ? "An outcome, on the record."
                                : "What changed in the real world?"
                            }
                          />
                          {mission.outcome ? (
                            <article className="outcome-record">
                              <span className="outcome-seal">
                                <CheckCheck size={26} />
                              </span>
                              <Badge tone="sage">Mission completed</Badge>
                              <h3>{mission.outcome.summary}</h3>
                              <p>{mission.outcome.verification}</p>
                              <small>
                                {mission.outcome.recorded_by} ·{" "}
                                {date(mission.outcome.recorded_at)}
                              </small>
                            </article>
                          ) : (
                            <>
                              <p className="section-intro">
                                Record what was delivered and how it was
                                verified. A submitted request does not count as
                                a completed outcome.
                              </p>
                              <div className="completion-summary">
                                <span>
                                  {done}
                                  <small> / {mission.tasks.length}</small>
                                </span>
                                <div>
                                  <strong>Tasks have confirmed results</strong>
                                  <p>
                                    {current
                                      ? "The plan reflects the current evidence."
                                      : "A current evidence plan is also required."}
                                  </p>
                                </div>
                              </div>
                              {done === mission.tasks.length &&
                              current &&
                              write ? (
                                <Form
                                  busy={busy}
                                  label="Record outcome & close mission"
                                  onSubmit={(data) =>
                                    execute({ type: "record_outcome", ...data })
                                  }
                                >
                                  <Field
                                    label="What changed?"
                                    name="summary"
                                    multiline
                                    minLength={10}
                                    maxLength={2000}
                                  />
                                  <Field
                                    label="How was the result verified?"
                                    name="verification"
                                    multiline
                                    minLength={10}
                                    maxLength={2000}
                                  />
                                </Form>
                              ) : (
                                <div className="notice">
                                  Confirm all task results and prepare a current
                                  plan to unlock the outcome record.
                                </div>
                              )}
                            </>
                          )}
                          <a
                            className="button secondary export-button"
                            href={`/api/vigil/missions/${mission.id}/export`}
                            download
                          >
                            <ArrowDownToLine size={16} />
                            Export mission & audit record
                          </a>
                        </>
                      )}
                    </div>
                    <footer className="mission-footer">
                      <span>
                        <Fingerprint size={13} />
                        Preserved originals. Recorded decisions.
                      </span>
                      <span>Updated {date(mission.updated_at)} SAST</span>
                    </footer>
                  </section>
                ) : (
                  <Empty title="Open the first mission.">
                    Define an objective and connect the first source.
                  </Empty>
                )}
              </div>
            </>
          )}
          <footer className="page-footer">
            <span>BUILT FOR THE WORK THAT CANNOT BE LEFT UNFINISHED.</span>
            <span>VIGIL / SOUTH AFRICA</span>
          </footer>
        </main>
      </div>
      {toast && (
        <div className="toast" role="status">
          <CircleCheck size={18} />
          {toast}
        </div>
      )}
      {modal && (
        <Dialog
          title={
            modal.type === "create"
              ? "Open a mission"
              : modal.type === "source"
                ? "Add a source"
                : modal.type === "task"
                  ? actionLabels[modal.target]
                  : "Inspect the evidence"
          }
          close={() => {
            if (!busy) setModal(null);
          }}
        >
          {modal.type === "create" && (
            <Form
              busy={busy}
              label="Open mission"
              onSubmit={async (data) => {
                await execute(data, true);
                setModal(null);
                setTab("Situation");
              }}
            >
              <div className="notice">
                Use fictional information in this demonstration workspace.
              </div>
              <Field
                label="Mission title"
                name="title"
                minLength={5}
                maxLength={160}
                placeholder="What needs to change?"
              />
              <Field
                label="Objective"
                name="objective"
                multiline
                minLength={10}
                maxLength={2000}
                placeholder="Define a verifiable result."
              />
              <div className="field-pair">
                <Field label="Mission family" name="domain">
                  <select name="domain">
                    {(identity?.actor === "protection"
                      ? ["PROTECT"]
                      : ["LIFELINES", "RESPONSE", "INTEGRITY", "INVESTIGATE"]
                    ).map((d) => (
                      <option key={d} value={d}>
                        {pretty(d)}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Priority" name="priority">
                  <select name="priority">
                    <option value="STANDARD">Standard</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </Field>
              </div>
              <Field
                label="Area or protected location label"
                name="location"
                minLength={2}
                maxLength={100}
              />
            </Form>
          )}
          {modal.type === "source" && (
            <Form
              busy={busy}
              label="Preserve source"
              onSubmit={async (data) => {
                await execute({ type: "add_evidence", ...data });
                setModal(null);
              }}
            >
              <Field
                label="Source title"
                name="title"
                minLength={3}
                maxLength={160}
              />
              <div className="field-pair">
                <Field
                  label="Source or custodian"
                  name="source"
                  minLength={2}
                  maxLength={160}
                />
                <Field label="Record type" name="kind">
                  <select name="kind">
                    {[
                      "FIELD_REPORT",
                      "DOCUMENT",
                      "SENSOR",
                      "SERVICE_UPDATE",
                      "STATEMENT",
                    ].map((d) => (
                      <option key={d} value={d}>
                        {pretty(d)}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
              <Field
                label="What does this source report?"
                name="statement"
                multiline
                minLength={5}
                maxLength={2000}
              />
              <Field
                label="Original text to preserve"
                name="original"
                multiline
                minLength={5}
                maxLength={16000}
              />
              <div className="notice">
                New sources begin as unverified. Adding a source invalidates the
                current plan.
              </div>
            </Form>
          )}
          {modal.type === "evidence" && mission && (
            <EvidenceDetail
              evidence={mission.evidence.find((e) => e.id === modal.id)!}
              write={write}
              busy={busy}
              save={async (data) => {
                await execute({
                  type: "review_evidence",
                  evidence_id: modal.id,
                  ...data,
                });
                setModal(null);
              }}
            />
          )}
          {modal.type === "task" && (
            <Form
              busy={busy}
              label={actionLabels[modal.target]}
              onSubmit={async (data) => {
                await execute({
                  type: "transition_task",
                  task_id: modal.task.id,
                  state: modal.target,
                  ...data,
                });
                setModal(null);
              }}
            >
              <div className="transition-title">
                <h3>{modal.task.title}</h3>
                <div>
                  <Badge>{stateLabels[modal.task.state]}</Badge>
                  <ArrowRight size={16} />
                  <Badge tone="sage">{stateLabels[modal.target]}</Badge>
                </div>
              </div>
              <div className="notice">
                Record a fictional receipt in this demo. This action updates the
                record; it does not contact a receiving team.
              </div>
              <Field
                label={
                  modal.target === "ACCEPTED"
                    ? "Who accepted the work, and what did they confirm?"
                    : modal.target === "CONFIRMED"
                      ? "What evidence confirms the result?"
                      : "Receipt or reason for this transition"
                }
                name="receipt"
                multiline
                minLength={5}
                maxLength={1000}
              />
            </Form>
          )}
        </Dialog>
      )}
    </div>
  );
}
