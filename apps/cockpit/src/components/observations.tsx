// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Source coverage and event-time workbench
"use client";
import { useRef, useState } from "react";
import { Plus, RefreshCw, MapPin } from "lucide-react";
import { api } from "@/lib/api";
import type { Identity } from "@/lib/contracts";
import {
  isoLocal,
  localInput,
  mapPositions,
  orderedObservations,
  type Capability,
  type Observation,
  type ObservationSearch,
} from "@/lib/coordination";
import { date, pretty } from "@/lib/presentation";
import { Dialog, Field } from "./primitives";
import {
  ExchangeForm,
  ObservationOriginal,
  useCoordination,
} from "./coordination-shared";

export function Observations({
  identity,
  onBusy,
  openInbox,
}: {
  identity: Identity;
  onBusy: (busy: boolean) => void;
  openInbox: () => void;
}) {
  const { data, error, busy, loading, refresh, mutate } =
    useCoordination(onBusy);
  const [selected, setSelected] = useState(""),
    [sourceId, setSourceId] = useState(""),
    [view, setView] = useState("timeline"),
    [modal, setModal] = useState<"source" | "observation" | Capability | null>(
      null,
    ),
    [results, setResults] = useState<ObservationSearch | null>(null),
    [queryError, setQueryError] = useState(""),
    [searching, setSearching] = useState(false);
  const searchSequence = useRef(0);
  const [openedAt] = useState(Date.now);
  const rows = orderedObservations(
    (results?.observations || data?.observations || []).filter(
      (o) => !sourceId || o.source_id === sourceId,
    ),
  );
  const observation = rows.find((o) => o.id === selected) || rows[0];
  const write = !identity.read_only;
  if (!data)
    return (
      <p role={error ? "alert" : "status"}>
        {error || "Loading observations…"}
      </p>
    );
  async function search(form: Record<string, string>) {
    const serial = ++searchSequence.current;
    setSearching(true);
    setQueryError("");
    try {
      const result = await api<ObservationSearch>("coordination/search", {
        from: isoLocal(form.from),
        to: isoLocal(form.to),
        query: form.query,
        ...(sourceId ? { source_id: sourceId } : {}),
      });
      if (serial === searchSequence.current) setResults(result);
    } catch (e) {
      if (serial === searchSequence.current)
        setQueryError((e as Error).message);
    } finally {
      if (serial === searchSequence.current) setSearching(false);
    }
  }
  return (
    <div className="coordination-workspace">
      {error && (
        <p className="notice danger" role="alert">
          {error}
        </p>
      )}
      <div className="coordination-toolbar">
        <label className="field">
          <span>Source</span>
          <select
            aria-label="Observation source"
            value={sourceId}
            disabled={busy || searching}
            onChange={(e) => {
              setSourceId(e.target.value);
              setResults(null);
            }}
          >
            <option value="">All owned sources</option>
            {data.sources.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <button
          className="button"
          disabled={busy || loading || searching}
          onClick={() => {
            setResults(null);
            void refresh().catch(() => {});
          }}
        >
          <RefreshCw size={15} />
          Refresh
        </button>
        <button
          className="button"
          disabled={!write || busy}
          onClick={() => setModal("source")}
        >
          <Plus size={15} />
          Register source
        </button>
        <button
          className="button primary"
          disabled={!write || busy || !data.sources.length}
          onClick={() => setModal("observation")}
        >
          <Plus size={15} />
          Record observation
        </button>
        <button className="text-button" disabled={busy} onClick={openInbox}>
          Agency inbox →
        </button>
      </div>
      <form
        className="observation-search"
        onSubmit={(e) => {
          e.preventDefault();
          void search(
            Object.fromEntries(new FormData(e.currentTarget)) as Record<
              string,
              string
            >,
          );
        }}
      >
        <label className="field">
          <span>Event time from</span>
          <input
            type="datetime-local"
            name="from"
            required
            defaultValue={localInput(new Date(openedAt - 86400000))}
          />
        </label>
        <label className="field">
          <span>Event time to</span>
          <input
            type="datetime-local"
            name="to"
            required
            defaultValue={localInput(new Date(openedAt + 3600000))}
          />
        </label>
        <label className="field">
          <span>Description or original text</span>
          <input
            name="query"
            maxLength={160}
            placeholder="Search observations…"
          />
        </label>
        <button className="button" disabled={busy || searching}>
          {searching ? "Searching…" : "Search window"}
        </button>
        {results && (
          <button
            type="button"
            className="text-button"
            disabled={searching}
            onClick={() => setResults(null)}
          >
            Clear search
          </button>
        )}
      </form>
      {queryError && (
        <p role="alert" className="notice danger">
          {queryError}
        </p>
      )}
      {results && (
        <p className="coordination-note">
          {results.observations.length} matches · {date(results.from)} to{" "}
          {date(results.to)} · searched {date(results.queried_at)}
        </p>
      )}
      <section className="coverage-register" aria-label="Source coverage">
        <div className="coordination-section-heading">
          <h2>Source coverage</h2>
          <span>Declared coverage · synthetic records</span>
        </div>
        <div className="coordination-table-wrap">
          <table>
            <thead>
              <tr>
                <th>Source / capability</th>
                <th>Availability</th>
                <th>Coverage and limitation</th>
                <th>
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {data.sources
                .filter((s) => !sourceId || s.id === sourceId)
                .map((s) => {
                  const coverage = results?.coverage.find(
                    (c) => c.source_id === s.id,
                  );
                  return (
                    <tr key={s.id}>
                      <td>
                        <strong>{s.name}</strong>
                        <small>
                          {pretty(s.kind)} · {s.capability}
                        </small>
                      </td>
                      <td>
                        <span
                          className={`availability ${s.status.toLowerCase()}`}
                        >
                          {pretty(coverage?.outcome || s.status)}
                        </span>
                        {coverage && (
                          <small>
                            {coverage.matches} matching observations
                          </small>
                        )}
                      </td>
                      <td>
                        {date(s.coverage_from)} — {date(s.coverage_to)}
                        <small>{s.status_note}</small>
                      </td>
                      <td>
                        <button
                          className="text-button"
                          disabled={!write || busy}
                          onClick={() => setModal(s)}
                        >
                          Update
                        </button>
                      </td>
                    </tr>
                  );
                })}
            </tbody>
          </table>
        </div>
        {!data.sources.length && (
          <p className="coordination-note">
            No sources registered in this workspace. The Gauteng and Limpopo
            demonstration workspaces include fictional corridor records.
          </p>
        )}
      </section>
      <div className="observation-layout">
        <section
          className="observation-explorer"
          aria-label="Observation explorer"
        >
          <div className="coordination-section-heading">
            <h2>
              {rows.length} observation{rows.length === 1 ? "" : "s"}
            </h2>
            <div
              className="segmented"
              role="group"
              aria-label="Observation view"
            >
              {["timeline", "map"].map((mode) => (
                <button
                  key={mode}
                  aria-pressed={view === mode}
                  onClick={() => setView(mode)}
                >
                  {mode === "timeline" ? "Timeline" : "Coverage map"}
                </button>
              ))}
            </div>
          </div>
          {view === "map" && (
            <CoverageMap
              sources={data.sources.filter(
                (s) => !sourceId || s.id === sourceId,
              )}
              observations={rows}
              select={setSelected}
            />
          )}
          <ol className="observation-timeline">
            {rows.map((o) => (
              <li key={o.id}>
                <button
                  className={o.id === observation?.id ? "selected" : ""}
                  onClick={() => setSelected(o.id)}
                >
                  <time>{date(o.occurred_at)}</time>
                  <span>
                    <strong>{o.description}</strong>
                    <small>
                      {o.source_name}
                      {o.clock_uncertainty_seconds
                        ? ` · clock ±${o.clock_uncertainty_seconds}s`
                        : ""}
                    </small>
                    {!!o.flags.length && (
                      <small>{o.flags.map(pretty).join(" · ")}</small>
                    )}
                  </span>
                </button>
              </li>
            ))}
          </ol>
          {!rows.length && (
            <p className="coordination-note">
              No observations match this view. Inspect source coverage before
              drawing conclusions.
            </p>
          )}
        </section>
        <aside
          className="observation-inspector"
          aria-label="Observation inspector"
        >
          {observation ? (
            <ObservationOriginal observation={observation} />
          ) : (
            <p>Select an observation to inspect its original record.</p>
          )}
        </aside>
      </div>
      {modal && (
        <Dialog
          title={
            modal === "source"
              ? "Register a source"
              : modal === "observation"
                ? "Record an observation"
                : "Update source availability"
          }
          close={() => {
            if (!busy) setModal(null);
          }}
        >
          <ExchangeForm
            busy={busy}
            label={
              modal === "observation"
                ? "Preserve observation"
                : modal === "source"
                  ? "Register source"
                  : "Record availability"
            }
            onSubmit={async (form) => {
              if (modal === "source")
                await mutate("sources", {
                  ...form,
                  latitude: Number(form.latitude),
                  longitude: Number(form.longitude),
                  coverage_from: isoLocal(form.coverage_from),
                  coverage_to: isoLocal(form.coverage_to),
                });
              else if (modal === "observation") {
                const result = await mutate<Observation>("observations", {
                  ...form,
                  occurred_at: isoLocal(form.occurred_at),
                  clock_uncertainty_seconds: Number(
                    form.clock_uncertainty_seconds,
                  ),
                  ...(form.supersedes_id ? {} : { supersedes_id: undefined }),
                });
                setSelected(result.id);
              } else
                await mutate(`sources/${modal.id}/status`, {
                  ...form,
                  expected_version: modal.version,
                });
              setResults(null);
              setModal(null);
            }}
          >
            {modal === "source" ? (
              <>
                <Field
                  name="name"
                  label="Source name"
                  minLength={3}
                  maxLength={160}
                />
                <Field name="kind" label="Source kind">
                  <select name="kind">
                    {["CAMERA", "TOLL", "FIELD_REPORT", "ACCESS_LOG"].map(
                      (k) => (
                        <option key={k}>{k}</option>
                      ),
                    )}
                  </select>
                </Field>
                <Field
                  name="location"
                  label="Location"
                  minLength={3}
                  maxLength={160}
                />
                <div className="form-row">
                  <label className="field">
                    <span>Latitude</span>
                    <input
                      type="number"
                      name="latitude"
                      min={-90}
                      max={90}
                      step="any"
                      required
                    />
                  </label>
                  <label className="field">
                    <span>Longitude</span>
                    <input
                      type="number"
                      name="longitude"
                      min={-180}
                      max={180}
                      step="any"
                      required
                    />
                  </label>
                </div>
                <Field
                  name="capability"
                  label="What this source can provide"
                  minLength={5}
                  maxLength={500}
                  multiline
                />
                <div className="form-row">
                  <label className="field">
                    <span>Coverage from</span>
                    <input
                      name="coverage_from"
                      type="datetime-local"
                      required
                    />
                  </label>
                  <label className="field">
                    <span>Coverage to</span>
                    <input name="coverage_to" type="datetime-local" required />
                  </label>
                </div>
              </>
            ) : modal === "observation" ? (
              <>
                <Field name="source_id" label="Owning source">
                  <select
                    name="source_id"
                    defaultValue={sourceId || data.sources[0]?.id}
                  >
                    {data.sources.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field
                  name="producer_event_id"
                  label="Producer event reference"
                  minLength={1}
                  maxLength={120}
                  placeholder="A stable reference from the originating source"
                />
                <div className="form-row">
                  <label className="field">
                    <span>Event time</span>
                    <input
                      type="datetime-local"
                      name="occurred_at"
                      defaultValue={localInput()}
                      required
                    />
                  </label>
                  <label className="field">
                    <span>Clock uncertainty (seconds)</span>
                    <input
                      type="number"
                      name="clock_uncertainty_seconds"
                      defaultValue={0}
                      min={0}
                      max={86400}
                      required
                    />
                  </label>
                </div>
                <Field
                  name="description"
                  label="Observed description"
                  minLength={5}
                  maxLength={500}
                  multiline
                />
                <Field
                  name="original"
                  label="Original source text"
                  minLength={5}
                  maxLength={8000}
                  multiline
                />
                <Field
                  name="supersedes_id"
                  label="Corrects observation (optional)"
                  required={false}
                  maxLength={80}
                  placeholder="OBS-…"
                />
                <p className="coordination-note">
                  The receipt time is recorded by Vigil. A correction preserves
                  the earlier observation.
                </p>
              </>
            ) : null}
            {modal !== "observation" && (
              <>
                <Field name="status" label="Availability">
                  <select
                    name="status"
                    defaultValue={
                      typeof modal === "object" ? modal.status : "NOT_CONNECTED"
                    }
                  >
                    {["AVAILABLE", "PARTIAL", "OFFLINE", "NOT_CONNECTED"].map(
                      (s) => (
                        <option key={s} value={s}>
                          {pretty(s)}
                        </option>
                      ),
                    )}
                  </select>
                </Field>
                <Field
                  name="status_note"
                  label="Coverage limitation / explanation"
                  minLength={5}
                  maxLength={500}
                  multiline
                  defaultValue={
                    typeof modal === "object" ? modal.status_note : ""
                  }
                />
              </>
            )}
          </ExchangeForm>
        </Dialog>
      )}
    </div>
  );
}
function CoverageMap({
  sources,
  observations,
  select,
}: {
  sources: Capability[];
  observations: Observation[];
  select: (id: string) => void;
}) {
  return (
    <div className="coverage-map">
      <svg
        viewBox="0 0 580 370"
        role="group"
        aria-label="Schematic source coverage map"
      >
        <rect
          x="1"
          y="1"
          width="578"
          height="368"
          fill="var(--paper)"
          stroke="var(--line)"
        />
        {[95, 185, 275].map((y) => (
          <line key={y} x1="0" x2="580" y1={y} y2={y} stroke="var(--line)" />
        ))}
        <text x="20" y="26" className="map-caption">
          N ↑ · Source positions
        </text>
        {mapPositions(sources).map(({ source, x, y }) => {
          const first = observations.find((o) => o.source_id === source.id);
          return (
            <g
              key={source.id}
              role="button"
              tabIndex={0}
              aria-label={`${source.name}, ${pretty(source.status)}, ${observations.filter((o) => o.source_id === source.id).length} observations`}
              onClick={() => {
                if (first) select(first.id);
              }}
              onKeyDown={(e) => {
                if (first && (e.key === "Enter" || e.key === " ")) {
                  e.preventDefault();
                  select(first.id);
                }
              }}
            >
              <circle
                cx={x}
                cy={y}
                r="12"
                fill={
                  source.status === "AVAILABLE"
                    ? "var(--forest)"
                    : "var(--orange)"
                }
              />
              <circle cx={x} cy={y} r="4" fill="var(--white)" />
              <text x={x} y={y + 28} textAnchor="middle">
                {source.name}
              </text>
            </g>
          );
        })}
      </svg>
      <p>
        <MapPin size={13} />
        Illustrative positions. No route inference or live map layer.
      </p>
    </div>
  );
}
