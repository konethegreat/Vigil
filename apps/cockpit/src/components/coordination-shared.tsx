// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Shared coordination loading, forms and evidence reading
"use client";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { api } from "@/lib/api";
import type { CoordinationWorkspace, Observation } from "@/lib/coordination";
import { date, pretty } from "@/lib/presentation";
export function useCoordination(onBusy: (busy: boolean) => void) {
  const [data, setData] = useState<CoordinationWorkspace | null>(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(true);
  const sequence = useRef(0),
    pending = useRef<{ signature: string; key: string } | null>(null),
    writing = useRef(false);
  const refresh = useCallback(async () => {
    const serial = ++sequence.current;
    setLoading(true);
    setError("");
    try {
      const result = await api<CoordinationWorkspace>("coordination/workspace");
      if (serial === sequence.current) setData(result);
    } catch (e) {
      if (serial === sequence.current) setError((e as Error).message);
      throw e;
    } finally {
      if (serial === sequence.current) setLoading(false);
    }
  }, []);
  useEffect(() => {
    const tracker = sequence;
    void refresh().catch(() => {});
    return () => {
      ++tracker.current;
    };
  }, [refresh]);
  async function mutate<T>(path: string, body: unknown): Promise<T> {
    if (writing.current)
      throw new Error("Wait for the current operation to finish.");
    const signature = JSON.stringify({ path, body });
    if (pending.current?.signature !== signature)
      pending.current = { signature, key: crypto.randomUUID() };
    writing.current = true;
    setBusy(true);
    onBusy(true);
    try {
      const result = await api<T>(
        `coordination/${path}`,
        body,
        pending.current.key,
      );
      pending.current = null;
      await refresh();
      return result;
    } finally {
      writing.current = false;
      setBusy(false);
      onBusy(false);
    }
  }
  return { data, error, busy, loading, refresh, mutate };
}
export function ExchangeForm({
  children,
  busy,
  onSubmit,
  label,
}: {
  children: ReactNode;
  busy: boolean;
  onSubmit: (data: Record<string, string>) => Promise<void>;
  label: string;
}) {
  const [error, setError] = useState("");
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    try {
      await onSubmit(
        Object.fromEntries(new FormData(e.currentTarget)) as Record<
          string,
          string
        >,
      );
    } catch (err) {
      setError((err as Error).message);
    }
  }
  return (
    <form className="record-form exchange-form" onSubmit={submit}>
      <fieldset disabled={busy}>{children}</fieldset>
      {error && (
        <p className="notice danger" role="alert">
          {error}
        </p>
      )}
      <div className="form-footer">
        <span>A named actor and timestamp accompany this record.</span>
        <button className="button primary" disabled={busy}>
          {busy ? "Recording…" : label}
        </button>
      </div>
    </form>
  );
}
export function ObservationOriginal({
  observation,
}: {
  observation: Observation;
}) {
  return (
    <div className="observation-original">
      <p className="mono-label">{observation.id}</p>
      <h3>{observation.description}</h3>
      <dl className="observation-facts">
        <div>
          <dt>Source</dt>
          <dd>
            {observation.source_name} · revision {observation.source_version}
          </dd>
        </div>
        <div>
          <dt>Event time</dt>
          <dd>
            {date(observation.occurred_at)}
            {observation.clock_uncertainty_seconds > 0 &&
              ` · ±${observation.clock_uncertainty_seconds}s`}
          </dd>
        </div>
        <div>
          <dt>Received</dt>
          <dd>{date(observation.received_at)}</dd>
        </div>
        <div>
          <dt>Location</dt>
          <dd>{observation.location}</dd>
        </div>
      </dl>
      {!!observation.flags.length && (
        <p className="observation-flags">
          {observation.flags.map(pretty).join(" · ")}
        </p>
      )}
      {observation.supersedes_id && (
        <p>
          Corrects {observation.supersedes_id}. The prior record remains
          preserved.
        </p>
      )}
      <h4>Preserved original</h4>
      <pre>{observation.original}</pre>
      <details>
        <summary>Original fingerprint</summary>
        <code className="long-hash">{observation.sha256}</code>
        <p>
          SHA-256 of the preserved text. The fingerprint does not verify the
          claim.
        </p>
      </details>
    </div>
  );
}
export function ObservationPicker({
  observations,
  selected,
  setSelected,
}: {
  observations: Observation[];
  selected: string[];
  setSelected: (ids: string[]) => void;
}) {
  return (
    <fieldset className="observation-picker">
      <legend>Select evidence to release</legend>
      <p>
        Only these observations and their original text will be included.
        Maximum 20.
      </p>
      {observations.map((o) => (
        <label key={o.id}>
          <input
            type="checkbox"
            checked={selected.includes(o.id)}
            disabled={!selected.includes(o.id) && selected.length >= 20}
            onChange={(e) =>
              setSelected(
                e.target.checked
                  ? [...selected, o.id]
                  : selected.filter((id) => id !== o.id),
              )
            }
          />
          <span>
            <strong>{o.description}</strong>
            <small>
              {o.source_name} · {date(o.occurred_at)}
            </small>
            <details>
              <summary>Inspect original</summary>
              <pre>{o.original}</pre>
            </details>
          </span>
        </label>
      ))}
      {!observations.length && (
        <p>
          No owned observations are available. Preserve a source observation
          first.
        </p>
      )}
    </fieldset>
  );
}
