// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"use client";
import { FileText, Fingerprint } from "lucide-react";
import { Badge, Form, Field } from "./primitives";
import type { Evidence } from "@/lib/contracts";
import { date, pretty } from "@/lib/presentation";
export function EvidenceDetail({
  evidence: e,
  write,
  busy,
  save,
}: {
  evidence: Evidence;
  write: boolean;
  busy: boolean;
  save: (data: Record<string, string>) => Promise<void>;
}) {
  return (
    <div className="evidence-detail">
      <div className="evidence-detail-title">
        <h3>{e.title}</h3>
        <Badge
          tone={
            e.status === "DISPUTED"
              ? "orange"
              : e.status === "SUPPORTED"
                ? "sage"
                : "neutral"
          }
        >
          {pretty(e.status)}
        </Badge>
      </div>
      <p className="muted">
        {e.source} · {pretty(e.kind)} · recorded {date(e.recorded_at)} SAST
      </p>
      <details open className="original">
        <summary>
          <FileText size={15} />
          Preserved original
        </summary>
        <pre>{e.original}</pre>
        <div className="hash">
          <Fingerprint size={14} />
          <code>{e.sha256}</code>
        </div>
      </details>
      {write ? (
        <Form busy={busy} label="Record assessment" onSubmit={save}>
          <Field
            label="Current interpretation"
            name="statement"
            defaultValue={e.statement}
            multiline
            minLength={5}
            maxLength={2000}
          />
          <Field label="Assessment" name="status">
            <select name="status" defaultValue={e.status}>
              <option value="UNVERIFIED">Unverified</option>
              <option value="SUPPORTED">Supported</option>
              <option value="DISPUTED">Disputed</option>
            </select>
          </Field>
          <Field
            label="Reason for this assessment"
            name="note"
            minLength={5}
            maxLength={1000}
          />
          <div className="notice">
            Revising an assessment preserves the original and makes the previous
            plan stale.
          </div>
        </Form>
      ) : (
        <p>{e.statement}</p>
      )}
      {e.history.length > 0 && (
        <div className="revision-history">
          <h4>Previous assessments</h4>
          {[...e.history].reverse().map((h, i) => (
            <article key={i}>
              <Badge>{pretty(h.status)}</Badge>
              <p>{h.statement}</p>
              <small>
                {h.actor} · {date(h.changed_at)}
              </small>
              <p className="muted">Reason: {h.reason}</p>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
