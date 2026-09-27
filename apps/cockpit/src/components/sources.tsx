// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Source reading and analyst review
"use client";
import { useEffect, useRef, useState } from "react";
import { ArrowRight, Plus, RefreshCw } from "lucide-react";
import { api } from "@/lib/api";
import type { Identity, Mission } from "@/lib/contracts";
import {
  retainLinks,
  type Extraction,
  type ExtractionResult,
} from "@/lib/extractions";
import { date, pretty } from "@/lib/presentation";
import { Badge, Dialog, Empty, Spinner } from "./primitives";
import { SourceIntake } from "./source-intake";
import { EvidenceDetail } from "./evidence-detail";

export function Sources({
  identity,
  missions,
  onMissionChange,
  onBusy,
  refreshMissions,
  openInvestigation,
}: {
  identity: Identity;
  missions: Mission[];
  onMissionChange: (mission: Mission) => void;
  onBusy: (busy: boolean) => void;
  refreshMissions: () => Promise<void>;
  openInvestigation: (missionId: string, objectId: string) => void;
}) {
  const [missionId, setMissionId] = useState("");
  const [sourceId, setSourceId] = useState("");
  const [query, setQuery] = useState("");
  const [busy, setBusy] = useState(false);
  const [extracting, setExtracting] = useState(false);
  const [error, setError] = useState("");
  const [modal, setModal] = useState<"add" | "assess" | null>(null);
  const [runs, setRuns] = useState<{
    mission: string;
    version: number;
    rows: Extraction[];
  } | null>(null);
  const [reload, setReload] = useState(0);
  const pending = useRef<{ signature: string; key: string } | null>(null);
  const mission =
    missions.find((m) => m.id === missionId) ||
    missions.find((m) => m.status !== "COMPLETED") ||
    missions[0];
  const source =
    mission?.evidence.find((s) => s.id === sourceId) || mission?.evidence[0];
  const writable = !identity.read_only && mission?.status !== "COMPLETED";
  const ready =
    !!mission &&
    runs?.mission === mission.id &&
    runs?.version === mission.version;
  const sourceRuns = ready
    ? runs!.rows.filter((run) => run.evidence_id === source?.id)
    : [];
  const current = sourceRuns.find(
    (run) => run.evidence_version === mission?.evidence_version,
  );

  useEffect(() => {
    if (!mission) return;
    let cancelled = false;
    api<{ runs: Extraction[] }>(`missions/${mission.id}/extractions`)
      .then((result) => {
        if (!cancelled)
          setRuns({
            mission: mission.id,
            version: mission.version,
            rows: result.runs,
          });
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });
    return () => {
      cancelled = true;
    };
  }, [mission, reload]);

  async function command(
    path: string,
    body: Record<string, unknown>,
    extraction = false,
  ) {
    if (busy) throw new Error("Wait for the current operation to finish.");
    const signature = JSON.stringify({ path, body });
    if (pending.current?.signature !== signature)
      pending.current = { signature, key: crypto.randomUUID() };
    setBusy(true);
    onBusy(true);
    setError("");
    try {
      const result = await api<Mission | ExtractionResult>(
        path,
        body,
        pending.current.key,
      );
      const updated = extraction
        ? (result as ExtractionResult).mission
        : (result as Mission);
      pending.current = null;
      onMissionChange(updated);
      setMissionId(updated.id);
      if (!extraction && body.type === "add_evidence")
        setSourceId(updated.evidence.at(-1)!.id);
      setModal(null);
    } finally {
      setBusy(false);
      onBusy(false);
    }
  }

  async function extract() {
    if (!mission || !source) return;
    setExtracting(true);
    try {
      await command(
        "extractions",
        {
          mission_id: mission.id,
          evidence_id: source.id,
          expected_version: mission.version,
        },
        true,
      );
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setExtracting(false);
    }
  }

  async function refresh() {
    setError("");
    try {
      await refreshMissions();
      setReload((value) => value + 1);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="sources-workspace">
      <div className="source-toolbar">
        <label className="field">
          <span>Mission</span>
          <select
            aria-label="Source mission"
            value={mission?.id || ""}
            disabled={busy}
            onChange={(e) => {
              setMissionId(e.target.value);
              setSourceId("");
              setError("");
              pending.current = null;
            }}
          >
            {missions.map((m) => (
              <option key={m.id} value={m.id}>
                {m.id} · {m.title}
              </option>
            ))}
          </select>
        </label>
        <button className="button secondary" disabled={busy} onClick={refresh}>
          <RefreshCw size={14} /> Refresh records
        </button>
        <button
          className="button primary"
          disabled={!mission || !writable || busy}
          onClick={() => setModal("add")}
        >
          <Plus size={15} /> Add source
        </button>
      </div>
      {error && (
        <div role="alert" className="notice danger">
          {error}
        </div>
      )}
      {identity.read_only && (
        <div className="notice">
          Independent review · inspect sources and decisions. Publishing
          requires a writing identity.
        </div>
      )}
      {!mission ? (
        <Empty title="No missions available">
          Open a mission to preserve its first source.
        </Empty>
      ) : (
        <div className="source-layout">
          <aside className="source-register" aria-label="Source register">
            <div className="source-register-heading">
              <strong>
                {mission.evidence.length}{" "}
                {mission.evidence.length === 1 ? "source" : "sources"}
              </strong>
              <span>Revision {mission.evidence_version}</span>
            </div>
            <input
              aria-label="Find a source"
              placeholder="Find a source…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            {mission.evidence
              .filter((item) =>
                `${item.title} ${item.source} ${item.statement}`
                  .toLowerCase()
                  .includes(query.toLowerCase()),
              )
              .map((item) => (
                <button
                  key={item.id}
                  className={`source-register-row ${source?.id === item.id ? "selected" : ""}`}
                  disabled={busy}
                  onClick={() => {
                    setSourceId(item.id);
                    setError("");
                  }}
                >
                  <span>
                    {pretty(item.kind)} · {pretty(item.status)}
                  </span>
                  <strong>{item.title}</strong>
                  <small>{item.source}</small>
                </button>
              ))}
            {!mission.evidence.length && (
              <p className="source-help">
                Add a document, field report or statement.
              </p>
            )}
          </aside>
          {source ? (
            <section className="source-reader" aria-label="Selected source">
              <header className="source-heading">
                <div>
                  <p className="eyebrow">
                    {pretty(source.kind)} / {source.id}
                  </p>
                  <h2>{source.title}</h2>
                  <p>
                    {source.source} · Recorded {date(source.recorded_at)}
                  </p>
                </div>
                <Badge
                  tone={source.status === "DISPUTED" ? "orange" : "neutral"}
                >
                  {pretty(source.status)}
                </Badge>
              </header>
              <div className="source-columns">
                <div className="original-column">
                  <div className="source-section-heading">
                    <h3>Original text</h3>
                    <button
                      className="text-button"
                      disabled={busy}
                      onClick={() => setModal("assess")}
                    >
                      Source assessment <ArrowRight size={13} />
                    </button>
                  </div>
                  <pre className="source-original">{source.original}</pre>
                  <details className="source-fingerprint">
                    <summary>Text fingerprint &amp; assessment</summary>
                    <code>{source.sha256}</code>
                    <p>{source.statement}</p>
                    <p>
                      The fingerprint identifies this preserved text; it does
                      not establish the truth of its claims.
                    </p>
                  </details>
                </div>
                <div className="proposal-column">
                  <div className="source-section-heading">
                    <h3>Extraction review</h3>
                    {!current && (
                      <button
                        className="button secondary small"
                        disabled={!writable || busy || !ready}
                        onClick={extract}
                      >
                        {extracting ? (
                          <>
                            <Spinner /> Reading source…
                          </>
                        ) : (
                          "Extract proposals"
                        )}
                      </button>
                    )}
                  </div>
                  {!ready ? (
                    <p className="source-help">
                      {error
                        ? "Source reviews could not load. Refresh records to retry."
                        : "Loading source reviews…"}
                    </p>
                  ) : current ? (
                    <ProposalReview
                      key={current.id}
                      run={current}
                      writable={writable}
                      busy={busy}
                      publish={(body) =>
                        command(
                          `extractions/${current.id}/review`,
                          { ...body, expected_version: mission.version },
                          true,
                        )
                      }
                      openInvestigation={() =>
                        openInvestigation(
                          current.mission_id,
                          Object.values(
                            current.decision?.object_records || {},
                          )[0] || "",
                        )
                      }
                    />
                  ) : (
                    <p className="source-help">
                      Extract objects and relationships from this text using the
                      configured local model. Review the supporting quotations
                      before publishing records.
                    </p>
                  )}
                  {sourceRuns
                    .filter((run) => run.id !== current?.id)
                    .map((run) => (
                      <details className="prior-extraction" key={run.id}>
                        <summary>
                          Earlier extraction · evidence revision{" "}
                          {run.evidence_version} ·{" "}
                          {run.decision ? "Reviewed" : "Source changed"}
                        </summary>
                        <ProposalReview
                          run={run}
                          writable={false}
                          busy={busy}
                          publish={async () => {}}
                          openInvestigation={() =>
                            openInvestigation(
                              run.mission_id,
                              Object.values(
                                run.decision?.object_records || {},
                              )[0] || "",
                            )
                          }
                        />
                      </details>
                    ))}
                </div>
              </div>
            </section>
          ) : (
            <Empty title="No source selected">
              Preserve the first source to start extracting records.
            </Empty>
          )}
        </div>
      )}
      {modal && mission && (
        <Dialog
          title={modal === "add" ? "Add a source" : "Source assessment"}
          close={() => {
            if (!busy) setModal(null);
          }}
        >
          {modal === "add" ? (
            <SourceIntake
              busy={busy}
              save={(data) =>
                command(`missions/${mission.id}/commands`, {
                  ...data,
                  type: "add_evidence",
                  expected_version: mission.version,
                })
              }
            />
          ) : (
            source && (
              <EvidenceDetail
                evidence={source}
                write={writable}
                busy={busy}
                save={(data) =>
                  command(`missions/${mission.id}/commands`, {
                    ...data,
                    type: "review_evidence",
                    evidence_id: source.id,
                    expected_version: mission.version,
                  })
                }
              />
            )
          )}
        </Dialog>
      )}
    </div>
  );
}

function ProposalReview({
  run,
  writable,
  busy,
  publish,
  openInvestigation,
}: {
  run: Extraction;
  writable: boolean;
  busy: boolean;
  publish: (body: Record<string, unknown>) => Promise<void>;
  openInvestigation: () => void;
}) {
  const [objects, setObjects] = useState<string[]>([]);
  const [links, setLinks] = useState<string[]>([]);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const decided = !!run.decision;
  const editable = writable && !decided && !run.stale && !busy;
  function toggleObject(id: string) {
    const next = objects.includes(id)
      ? objects.filter((item) => item !== id)
      : [...objects, id];
    setObjects(next);
    setLinks(retainLinks(run.proposal.links, next, links));
  }
  return (
    <div className="proposal-review">
      <p className="source-help">
        {run.model} · {date(run.created_at)} · Evidence revision{" "}
        {run.evidence_version}
      </p>
      {run.stale && (
        <div className="notice warning">
          The evidence changed after this extraction. This proposal cannot be
          published.
        </div>
      )}
      {!decided && (
        <p className="proposal-instruction">
          Select the records supported by the source. Publication records a
          reported claim, not an independently verified fact.
        </p>
      )}
      <h4>Objects ({run.proposal.objects.length})</h4>
      {run.proposal.objects.map((item) => (
        <article className="proposal-row" key={item.id}>
          <label>
            <input
              type="checkbox"
              checked={
                decided
                  ? run.decision!.object_ids.includes(item.id)
                  : objects.includes(item.id)
              }
              disabled={!editable}
              onChange={() => toggleObject(item.id)}
            />
            <span>
              <small>
                {pretty(item.object_type)}
                {decided
                  ? ` · ${run.decision!.object_ids.includes(item.id) ? "Published" : "Rejected"}`
                  : ""}
              </small>
              <strong>{item.title}</strong>
            </span>
          </label>
          <p>{item.summary}</p>
          <blockquote>{item.quote}</blockquote>
        </article>
      ))}
      <h4>Relationships ({run.proposal.links.length})</h4>
      {run.proposal.links.map((item) => {
        const endpoints =
          objects.includes(item.from_id) && objects.includes(item.to_id);
        const title = (id: string) =>
          run.proposal.objects.find((o) => o.id === id)?.title || id;
        return (
          <article className="proposal-row" key={item.id}>
            <label>
              <input
                type="checkbox"
                checked={
                  decided
                    ? run.decision!.link_ids.includes(item.id)
                    : links.includes(item.id)
                }
                disabled={!editable || !endpoints}
                onChange={() =>
                  setLinks((current) =>
                    current.includes(item.id)
                      ? current.filter((id) => id !== item.id)
                      : [...current, item.id],
                  )
                }
              />
              <span>
                <small>
                  {pretty(item.relation)}
                  {decided
                    ? ` · ${run.decision!.link_ids.includes(item.id) ? "Published" : "Rejected"}`
                    : ""}
                </small>
                <strong>
                  {title(item.from_id)} → {title(item.to_id)}
                </strong>
              </span>
            </label>
            <p>{item.reason}</p>
            <blockquote>{item.quote}</blockquote>
            {!decided && !endpoints && (
              <small className="source-help">
                Select both objects to include this relationship.
              </small>
            )}
          </article>
        );
      })}
      {!!run.proposal.gaps.length && (
        <div className="proposal-concerns">
          <h4>Missing context</h4>
          <ul>
            {run.proposal.gaps.map((gap, i) => (
              <li key={i}>{gap}</li>
            ))}
          </ul>
        </div>
      )}
      <div className="proposal-concerns">
        <h4>Independent model review</h4>
        <p>
          {run.review.state === "NOT_RUN"
            ? "A second model did not review this proposal."
            : run.review.state === "UNAVAILABLE"
              ? `${run.review.model}: review could not complete.`
              : `${run.review.model}: ${run.review.concerns.length ? "concerns below." : "no concerns returned. Analyst review is still required."}`}
        </p>
        {!!run.review.concerns.length && (
          <ul>
            {run.review.concerns.map((concern, i) => (
              <li key={i}>{concern}</li>
            ))}
          </ul>
        )}
        {run.warnings.map((warning, i) => (
          <p key={i}>{warning}</p>
        ))}
      </div>
      {run.decision ? (
        <div className="review-receipt">
          <h4>Review recorded</h4>
          <p>{run.decision.note}</p>
          <small>
            {run.decision.actor} · {date(run.decision.reviewed_at)}
          </small>
          <p>
            {run.decision.object_ids.length} objects and{" "}
            {run.decision.link_ids.length} relationships published. Unselected
            proposals were rejected.
          </p>
          {!!run.decision.object_ids.length && (
            <button className="text-button" onClick={openInvestigation}>
              Open investigations <ArrowRight size={14} />
            </button>
          )}
        </div>
      ) : (
        writable &&
        !run.stale && (
          <form
            className="proposal-decision"
            onSubmit={async (e) => {
              e.preventDefault();
              setError("");
              try {
                await publish({ object_ids: objects, link_ids: links, note });
              } catch (err) {
                setError((err as Error).message);
              }
            }}
          >
            <label className="field">
              <span>Review note</span>
              <textarea
                required
                minLength={5}
                maxLength={1000}
                rows={3}
                disabled={busy}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Explain what the source supports and what remains uncertain."
              />
            </label>
            <p className="source-help">
              {objects.length} objects and {links.length} relationships
              selected. All other proposals will be rejected. The decision is
              final.
            </p>
            {error && (
              <div role="alert" className="notice danger">
                {error}
              </div>
            )}
            <button className="button primary" disabled={busy}>
              {busy
                ? "Recording review…"
                : objects.length
                  ? "Publish selected records"
                  : "Reject all proposals"}
            </button>
          </form>
        )
      )}
    </div>
  );
}
