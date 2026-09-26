// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Connected investigation workspace
"use client";
import { useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  Box,
  Clock3,
  FileSearch,
  GitBranch,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
} from "lucide-react";
import type { Evidence, Identity, Mission } from "@/lib/contracts";
import { api } from "@/lib/api";
import { date, pretty } from "@/lib/presentation";
import {
  objectTypes,
  relations,
  projectGraph,
  timelineObjects,
  type InvestigationGraph,
  type InvestigationObject,
  type InvestigationLink,
  type SourcedRecord,
} from "@/lib/investigations";
import { Badge, Dialog, Empty, Field, Form, Spinner } from "./primitives";
import { EvidenceDetail } from "./evidence-detail";

type Modal =
  | { kind: "object" | "link" }
  | { kind: "review"; link: InvestigationLink }
  | { kind: "source"; evidence: Evidence }
  | null;
const emptyGraph: InvestigationGraph = {
  objects: [],
  links: [],
  object_limit: 500,
  link_limit: 1000,
  synthetic: true,
};

export function Investigations({
  identity,
  missions,
  onMissionChange,
  openMission,
  onBusy,
  refreshMissions,
}: {
  identity: Identity;
  missions: Mission[];
  onMissionChange: (mission: Mission) => void;
  openMission: (id: string) => void;
  onBusy: (busy: boolean) => void;
  refreshMissions: () => Promise<void>;
}) {
  const [graph, setGraph] = useState<InvestigationGraph>(emptyGraph);
  const [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [scope, setScope] = useState(""),
    [query, setQuery] = useState(""),
    [type, setType] = useState("");
  const [view, setView] = useState<"Network" | "Timeline" | "Objects">(
    "Network",
  );
  const [selectedId, setSelectedId] = useState(""),
    [focus, setFocus] = useState(false),
    [modal, setModal] = useState<Modal>(null);
  const [formMission, setFormMission] = useState(""),
    [formType, setFormType] = useState("ASSET");
  const pending = useRef<{ signature: string; key: string } | null>(null);
  useEffect(() => {
    let cancelled = false;
    api<InvestigationGraph>("investigations")
      .then((value) => {
        if (!cancelled) setGraph(value);
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);
  const projected = projectGraph(graph, scope, query, type);
  const selected =
    graph.objects.find((object) => object.id === selectedId) ||
    projected.objects[0];
  const related = selected
    ? graph.links.filter(
        (link) => link.from_id === selected.id || link.to_id === selected.id,
      )
    : [];
  const neighbourIds = new Set(
    related.flatMap((link) => [link.from_id, link.to_id]),
  );
  const displayed =
    focus && selected
      ? projected.objects.filter(
          (object) => object.id === selected.id || neighbourIds.has(object.id),
        )
      : projected.objects;
  const nodeIds = new Set(displayed.slice(0, 30).map((object) => object.id));
  const visibleLinks = projected.links.filter(
    (link) => nodeIds.has(link.from_id) && nodeIds.has(link.to_id),
  );
  const activeMissions = missions.filter(
    (mission) => mission.status !== "COMPLETED" && mission.evidence.length,
  );
  const writingMission = missions.find((mission) => mission.id === formMission);
  const availableSources = writingMission?.evidence || [];
  const writable =
    !identity.read_only && activeMissions.length > 0 && !loading && !busy;

  async function reload() {
    setLoading(true);
    setError("");
    try {
      const [value] = await Promise.all([
        api<InvestigationGraph>("investigations"),
        refreshMissions(),
      ]);
      setGraph(value);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }
  function create(kind: "object" | "link") {
    setFormMission(
      activeMissions.find((mission) => mission.id === scope)?.id ||
        activeMissions[0]?.id ||
        "",
    );
    setFormType("ASSET");
    setModal({ kind });
  }
  function inspectSource(record: SourcedRecord) {
    const evidence = missions
      .find((mission) => mission.id === record.mission_id)
      ?.evidence.find((source) => source.id === record.evidence_id);
    if (evidence) setModal({ kind: "source", evidence });
    else
      setError(
        "The cited source is no longer in the loaded workspace. Refresh the records.",
      );
  }
  async function execute(
    data: Record<string, string>,
    operation: string,
    link?: InvestigationLink,
  ) {
    const mission = missions.find(
      (item) => item.id === (link?.mission_id || formMission),
    );
    if (!mission || busy)
      throw new Error("Select an available mission and wait for pending work.");
    const body = {
      ...data,
      type: operation,
      mission_id: mission.id,
      expected_version: mission.version,
      ...(link ? { link_id: link.id, expected_revision: link.revision } : {}),
    };
    const signature = JSON.stringify(body);
    if (pending.current?.signature !== signature)
      pending.current = { signature, key: crypto.randomUUID() };
    setBusy(true);
    onBusy(true);
    try {
      const result = await api<{ record_id: string; mission: Mission }>(
        "investigations/commands",
        body,
        pending.current.key,
      );
      pending.current = null;
      onMissionChange(result.mission);
      setModal(null);
      if (operation === "create_object") {
        setSelectedId(result.record_id);
        setScope(mission.id);
        setQuery("");
        setType("");
        setFocus(false);
        setView("Objects");
      }
      try {
        setGraph(await api<InvestigationGraph>("investigations"));
      } catch {
        setError(
          "The change was saved, but the graph could not refresh. Use Refresh records.",
        );
      }
    } finally {
      setBusy(false);
      onBusy(false);
    }
  }
  function choose(id: string) {
    setSelectedId(id);
  }
  function changeScope(id: string) {
    setScope(id);
    setSelectedId("");
    setFocus(false);
  }

  return (
    <div className="investigation-workspace">
      <section
        className="investigation-summary"
        aria-label="Investigation coverage"
      >
        <div>
          <span className="eyebrow">ONE OPERATIONAL PICTURE</span>
          <p>Connect records. Test explanations. Direct the next move.</p>
        </div>
        <div>
          <strong>{graph.objects.length}</strong>
          <span>Source-backed objects</span>
        </div>
        <div>
          <strong>{graph.links.length}</strong>
          <span>Recorded relationships</span>
        </div>
        <div>
          <strong>
            {new Set(graph.objects.map((object) => object.mission_id)).size}
          </strong>
          <span>Connected missions</span>
        </div>
        <div className="attention">
          <strong>
            {
              graph.links.filter(
                (link) =>
                  link.assessment === "DISPUTED" ||
                  link.provenance.needs_review,
              ).length
            }
          </strong>
          <span>Disputed / needs review</span>
        </div>
      </section>
      <div className="investigation-toolbar">
        <label>
          <span>Investigation scope</span>
          <select
            aria-label="Investigation scope"
            value={scope}
            onChange={(event) => changeScope(event.target.value)}
          >
            <option value="">All visible missions</option>
            {missions.map((mission) => (
              <option key={mission.id} value={mission.id}>
                {mission.id} · {mission.title}
              </option>
            ))}
          </select>
        </label>
        <label className="object-search">
          <Search size={15} />
          <input
            aria-label="Search investigation objects"
            placeholder="Search objects, records, places…"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setSelectedId("");
              setFocus(false);
            }}
          />
        </label>
        <button className="button" onClick={reload} disabled={busy || loading}>
          <RefreshCw size={14} />
          Refresh records
        </button>
        <button
          className="button primary"
          onClick={() => create("object")}
          disabled={!writable}
        >
          <Plus size={15} />
          Add object
        </button>
      </div>
      {error && (
        <div role="alert" className="notice danger">
          {error}
        </div>
      )}
      {identity.read_only && (
        <div className="notice">
          <ShieldCheck size={15} /> Independent review · records can be
          inspected; changes require a writing identity.
        </div>
      )}
      {!activeMissions.length && !identity.read_only && (
        <div className="notice">
          Open a mission and add its first source to begin an investigation.
        </div>
      )}
      {loading && !graph.objects.length ? (
        <div className="loading">
          <Spinner /> Loading authorised investigation records…
        </div>
      ) : (
        <div className="investigation-layout">
          <section
            className="investigation-canvas"
            aria-label="Investigation explorer"
          >
            <div className="investigation-viewbar">
              <div role="group" aria-label="Investigation view">
                {(["Network", "Timeline", "Objects"] as const).map((name) => (
                  <button
                    key={name}
                    className={view === name ? "active" : ""}
                    aria-pressed={view === name}
                    onClick={() => setView(name)}
                  >
                    {name === "Network" ? (
                      <GitBranch size={14} />
                    ) : name === "Timeline" ? (
                      <Clock3 size={14} />
                    ) : (
                      <Box size={14} />
                    )}{" "}
                    {name}
                  </button>
                ))}
              </div>
              <select
                aria-label="Filter object type"
                value={type}
                onChange={(event) => {
                  setType(event.target.value);
                  setSelectedId("");
                  setFocus(false);
                }}
              >
                <option value="">All object types</option>
                {objectTypes.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
            {!projected.objects.length ? (
              <Empty title="No objects in this view.">
                Change the filters, or preserve an object from a mission source.
              </Empty>
            ) : view === "Network" ? (
              <>
                <div className="network-context">
                  <span>
                    <span className="connection-dot" /> {displayed.length}{" "}
                    objects · {visibleLinks.length} visible relationships
                  </span>
                  <label>
                    <input
                      type="checkbox"
                      checked={focus}
                      onChange={(event) => setFocus(event.target.checked)}
                    />{" "}
                    Selected object + neighbours
                  </label>
                </div>
                <Network
                  objects={displayed.slice(0, 30)}
                  links={visibleLinks}
                  selected={selected?.id}
                  choose={choose}
                />
                {displayed.length > 30 && (
                  <div className="notice">
                    Showing 30 of {displayed.length} objects. Narrow the filters
                    or use the Objects view for the full result.
                  </div>
                )}
                <div className="network-legend">
                  <span>
                    <i className="corroborated" /> Corroborated
                  </span>
                  <span>
                    <i /> Reported
                  </span>
                  <span>
                    <i className="disputed" /> Disputed / needs review
                  </span>
                </div>
                <p className="network-note">
                  Connections are recorded claims with sources. Position and
                  proximity carry no geographic, causal, or confidence meaning.
                </p>
              </>
            ) : view === "Timeline" ? (
              <div className="investigation-timeline">
                <div className="timeline-intro">
                  <Clock3 size={18} />
                  <p>
                    Occurrence time, where the source supplies it. Unknown times
                    remain explicit; sequence does not establish cause.
                  </p>
                </div>
                {!timelineObjects(projected.objects).length && (
                  <Empty title="No event records in this view.">
                    Add an Event object with its source and, where known, an
                    explicit time.
                  </Empty>
                )}
                {timelineObjects(projected.objects).map((object) => (
                  <button
                    key={object.id}
                    className={`timeline-record ${selected?.id === object.id ? "selected" : ""}`}
                    onClick={() => choose(object.id)}
                  >
                    <time>
                      {object.occurred_at
                        ? `${date(object.occurred_at)} SAST`
                        : "Occurrence time unknown"}
                    </time>
                    <div>
                      <strong>{object.title}</strong>
                      <p>{object.summary}</p>
                      <span>{object.provenance.evidence_title}</span>
                      {object.provenance.needs_review && (
                        <Badge tone="orange">Source changed</Badge>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <div className="investigation-object-list">
                {projected.objects.map((object) => (
                  <button
                    key={object.id}
                    onClick={() => choose(object.id)}
                    className={selected?.id === object.id ? "selected" : ""}
                  >
                    <span
                      className={`object-type type-${object.object_type.toLowerCase()}`}
                    >
                      {pretty(object.object_type)}
                    </span>
                    <div>
                      <strong>{object.title}</strong>
                      <p>{object.summary}</p>
                    </div>
                    <span>
                      {object.provenance.needs_review
                        ? "Needs review"
                        : pretty(object.provenance.evidence_status)}
                    </span>
                    <ArrowRight size={15} />
                  </button>
                ))}
              </div>
            )}
            <div className="investigation-canvas-footer">
              <span>
                {projected.objects.length} of {graph.objects.length} authorised
                objects
              </span>
              <span>Tenant-scoped · original sources preserved</span>
            </div>
          </section>
          <aside className="object-inspector" aria-label="Object inspector">
            {selected ? (
              <>
                <div className="inspector-heading">
                  <span
                    className={`object-type type-${selected.object_type.toLowerCase()}`}
                  >
                    {pretty(selected.object_type)}
                  </span>
                  <Box size={17} />
                </div>
                <h2>{selected.title}</h2>
                <p className="object-description">{selected.summary}</p>
                <code className="object-id">{selected.id}</code>
                {selected.provenance.needs_review && (
                  <div className="notice danger">
                    Mission evidence changed since this object was recorded.
                    Recheck its supporting material.
                  </div>
                )}
                <div className="inspector-source">
                  <span className="eyebrow">PRESERVED SOURCE</span>
                  <button onClick={() => inspectSource(selected)}>
                    <FileSearch size={18} />
                    <span>
                      {selected.provenance.evidence_title}
                      <small>
                        {pretty(selected.provenance.evidence_status)} · evidence
                        snapshot {selected.evidence_version}
                      </small>
                    </span>
                    <ArrowRight size={14} />
                  </button>
                </div>
                <div className="inspector-mission">
                  <span className="eyebrow">ORIGINATING MISSION</span>
                  <p>{selected.provenance.mission_title}</p>
                  <button
                    className="text-button"
                    onClick={() => openMission(selected.mission_id)}
                  >
                    Open mission <ArrowRight size={14} />
                  </button>
                </div>
                <div className="inspector-relations">
                  <div className="inspector-section-heading">
                    <h3>
                      Relationships <span>{related.length}</span>
                    </h3>
                    <button
                      className="icon-button"
                      aria-label="Add relationship"
                      onClick={() => create("link")}
                      disabled={!writable || graph.objects.length < 2}
                    >
                      <Plus size={17} />
                    </button>
                  </div>
                  <p className="muted">
                    All visible missions · directional, source-backed
                    assertions.
                  </p>
                  {!related.length && (
                    <p className="muted">
                      No relationships recorded for this object.
                    </p>
                  )}
                  {related.map((link) => (
                    <div className="relationship-card" key={link.id}>
                      <div>
                        <Badge
                          tone={
                            link.assessment === "DISPUTED" ||
                            link.provenance.needs_review
                              ? "orange"
                              : link.assessment === "CORROBORATED"
                                ? "sage"
                                : "neutral"
                          }
                        >
                          {pretty(link.assessment)}
                        </Badge>
                        {link.provenance.needs_review && (
                          <span className="review-needed">Needs review</span>
                        )}
                      </div>
                      <p className="relationship-direction">
                        {
                          graph.objects.find(
                            (object) => object.id === link.from_id,
                          )?.title
                        }{" "}
                        <b>{pretty(link.relation).toLowerCase()}</b>{" "}
                        {
                          graph.objects.find(
                            (object) => object.id === link.to_id,
                          )?.title
                        }
                      </p>
                      <p>{link.reason}</p>
                      <button
                        className="text-button source-reference"
                        onClick={() => inspectSource(link)}
                      >
                        <FileSearch size={13} />
                        {link.provenance.evidence_title}
                      </button>
                      <div className="relationship-actions">
                        <button
                          className="text-button"
                          onClick={() => {
                            setSelectedId(
                              link.from_id === selected.id
                                ? link.to_id
                                : link.from_id,
                            );
                            setScope("");
                            setType("");
                            setQuery("");
                          }}
                        >
                          Explore connection <ArrowRight size={12} />
                        </button>
                        <button
                          className="text-button"
                          disabled={
                            !writable ||
                            missions.find((m) => m.id === link.mission_id)
                              ?.status === "COMPLETED"
                          }
                          onClick={() => setModal({ kind: "review", link })}
                        >
                          Review
                        </button>
                      </div>
                      <details>
                        <summary>
                          Assessment history · {link.history.length}
                        </summary>
                        {link.history.map((item) => (
                          <div
                            className="assertion-history"
                            key={item.revision}
                          >
                            <strong>
                              {pretty(item.assessment)} · revision{" "}
                              {item.revision}
                            </strong>
                            <p>{item.reason}</p>
                            <small>
                              {item.actor} · {date(item.recorded_at)} SAST
                            </small>
                          </div>
                        ))}
                      </details>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <Empty title="Inspect an object.">
                Select a graph node, event, or object record to trace its
                sources and relationships.
              </Empty>
            )}
          </aside>
        </div>
      )}
      {modal && (
        <Dialog
          title={
            modal.kind === "object"
              ? "Preserve an investigation object"
              : modal.kind === "link"
                ? "Record a relationship"
                : modal.kind === "review"
                  ? "Review relationship"
                  : "Source record"
          }
          close={() => {
            if (!busy) setModal(null);
          }}
        >
          {modal.kind === "source" ? (
            <EvidenceDetail
              evidence={modal.evidence}
              write={false}
              busy={false}
              save={async () => {}}
            />
          ) : (
            <Form
              busy={busy}
              label={
                modal.kind === "review"
                  ? "Record review"
                  : "Save investigation record"
              }
              onSubmit={(data) =>
                execute(
                  data,
                  modal.kind === "object"
                    ? "create_object"
                    : modal.kind === "link"
                      ? "create_link"
                      : "review_link",
                  modal.kind === "review" ? modal.link : undefined,
                )
              }
            >
              {modal.kind !== "review" && (
                <>
                  <label className="field">
                    <span>Source mission</span>
                    <select
                      aria-label="Source mission"
                      value={formMission}
                      onChange={(event) => setFormMission(event.target.value)}
                      required
                    >
                      {activeMissions.map((mission) => (
                        <option key={mission.id} value={mission.id}>
                          {mission.id} · {mission.title}
                        </option>
                      ))}
                    </select>
                  </label>
                  <Field label="Supporting source" name="evidence_id">
                    <select name="evidence_id" key={formMission} required>
                      {availableSources.map((source) => (
                        <option key={source.id} value={source.id}>
                          {source.title} · {pretty(source.status)}
                        </option>
                      ))}
                    </select>
                  </Field>
                </>
              )}
              {modal.kind === "object" ? (
                <>
                  <Field label="Object type" name="object_type">
                    <select
                      name="object_type"
                      value={formType}
                      onChange={(event) => setFormType(event.target.value)}
                    >
                      {objectTypes.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </Field>
                  <Field
                    label="Object title"
                    name="title"
                    minLength={3}
                    maxLength={160}
                  />
                  <Field
                    label="Source-backed description"
                    name="summary"
                    multiline
                    minLength={5}
                    maxLength={2000}
                  />
                  {formType === "EVENT" && (
                    <Field
                      label="Occurrence time (ISO timestamp with timezone, optional)"
                      name="occurred_at"
                      required={false}
                      placeholder="2026-09-24T06:20:00+02:00"
                      maxLength={50}
                    />
                  )}
                </>
              ) : (
                <>
                  {modal.kind === "link" && (
                    <>
                      <Field label="From object" name="from_id">
                        <select
                          name="from_id"
                          defaultValue={selected?.id}
                          required
                        >
                          {graph.objects.map((object) => (
                            <option key={object.id} value={object.id}>
                              {pretty(object.object_type)} · {object.title}
                            </option>
                          ))}
                        </select>
                      </Field>
                      <Field label="Relationship" name="relation">
                        <select name="relation" defaultValue="RELATED_TO">
                          {relations.map((value) => (
                            <option key={value}>{value}</option>
                          ))}
                        </select>
                      </Field>
                      <Field label="To object" name="to_id">
                        <select
                          name="to_id"
                          defaultValue={
                            graph.objects.find(
                              (object) => object.id !== selected?.id,
                            )?.id
                          }
                          required
                        >
                          {graph.objects.map((object) => (
                            <option key={object.id} value={object.id}>
                              {pretty(object.object_type)} · {object.title}
                            </option>
                          ))}
                        </select>
                      </Field>
                      <p className="muted">
                        Direction and object types are validated. A claim must
                        be supported by the selected mission source.
                      </p>
                    </>
                  )}
                  {modal.kind === "review" && (
                    <p>
                      Review the assertion against{" "}
                      <strong>{modal.link.provenance.evidence_title}</strong>.
                      Earlier assessments remain in the history.
                    </p>
                  )}
                  <Field label="Relationship assessment" name="assessment">
                    <select
                      name="assessment"
                      defaultValue={
                        modal.kind === "review"
                          ? modal.link.assessment
                          : "REPORTED"
                      }
                    >
                      {["REPORTED", "CORROBORATED", "DISPUTED"].map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </Field>
                  <Field
                    label="Evidence and reasoning for this assessment"
                    name="reason"
                    multiline
                    minLength={5}
                    maxLength={2000}
                  />
                </>
              )}
            </Form>
          )}
        </Dialog>
      )}
    </div>
  );
}

function Network({
  objects,
  links,
  selected,
  choose,
}: {
  objects: InvestigationObject[];
  links: InvestigationLink[];
  selected?: string;
  choose: (id: string) => void;
}) {
  const centre = [...objects].sort(
    (a, b) =>
      links.filter((link) => link.from_id === b.id || link.to_id === b.id)
        .length -
      links.filter((link) => link.from_id === a.id || link.to_id === a.id)
        .length,
  )[0];
  const ordered = centre
    ? [centre, ...objects.filter((object) => object.id !== centre.id)]
    : [];
  const coordinates = new Map(
    ordered.map((object, index) => {
      const angle =
        ((index - 1) / Math.max(ordered.length - 1, 1)) * Math.PI * 2 -
        Math.PI / 2;
      return [
        object.id,
        {
          x: index === 0 ? 460 : 460 + Math.cos(angle) * 310,
          y: index === 0 ? 295 : 295 + Math.sin(angle) * 213,
        },
      ];
    }),
  );
  return (
    <div className="network-scroll">
      <svg
        className="investigation-network"
        role="group"
        viewBox="0 0 920 590"
        aria-label="Evidence-backed relationship network"
      >
        <defs>
          <pattern
            id="investigation-grid"
            width="24"
            height="24"
            patternUnits="userSpaceOnUse"
          >
            <circle cx="1" cy="1" r="0.6" fill="#799681" opacity="0.3" />
          </pattern>
          <marker
            id="relationship-arrow"
            viewBox="0 0 10 10"
            refX="8"
            refY="5"
            markerWidth="5"
            markerHeight="5"
            orient="auto-start-reverse"
          >
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#9cae9b" />
          </marker>
        </defs>
        <rect width="920" height="590" fill="url(#investigation-grid)" />
        {links.map((link) => {
          const a = coordinates.get(link.from_id),
            b = coordinates.get(link.to_id);
          if (!a || !b) return null;
          const dx = b.x - a.x,
            dy = b.y - a.y,
            distance = Math.hypot(dx, dy) || 1;
          return (
            <g
              key={link.id}
              className={`network-edge ${link.assessment.toLowerCase()} ${link.provenance.needs_review ? "stale" : ""}`}
            >
              <line
                x1={a.x}
                y1={a.y}
                x2={b.x - (dx / distance) * 77}
                y2={b.y - (dy / distance) * 46}
                markerEnd="url(#relationship-arrow)"
              />
              <title>
                {pretty(link.relation)} · {pretty(link.assessment)} ·{" "}
                {link.provenance.evidence_title}
              </title>
            </g>
          );
        })}
        {ordered.map((object) => {
          const point = coordinates.get(object.id)!;
          return (
            <g
              key={object.id}
              transform={`translate(${point.x - 92} ${point.y - 38})`}
              role="button"
              tabIndex={0}
              aria-label={`Inspect ${object.title}`}
              aria-pressed={selected === object.id}
              className={`network-node ${selected === object.id ? "selected" : ""}`}
              onClick={() => choose(object.id)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  choose(object.id);
                }
              }}
            >
              <rect width="184" height="76" rx="5" />
              <text x="14" y="21" className="network-node-type">
                {object.object_type}
              </text>
              <text x="14" y="42">
                {object.title.length > 24
                  ? object.title.slice(0, 23) + "…"
                  : object.title}
              </text>
              <text x="14" y="60" className="network-node-source">
                {object.provenance.needs_review
                  ? "EVIDENCE CHANGED"
                  : object.provenance.evidence_status}
              </text>
              <title>{object.title}</title>
            </g>
          );
        })}
      </svg>
    </div>
  );
}
