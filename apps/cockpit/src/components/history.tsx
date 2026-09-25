// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"use client";
import { Fingerprint, Radio, Circle } from "lucide-react";
import { Badge, Spinner } from "./primitives";
import type { Audit, Mission } from "@/lib/contracts";
import { date, pretty } from "@/lib/presentation";
export function HistoryView({
  audit,
  error,
  mission,
  missions,
  select,
}: {
  audit: Audit | null;
  error: string;
  mission: Mission | undefined;
  missions: Mission[];
  select: (id: string) => void;
}) {
  return (
    <section className="history-view">
      <label className="field history-selector">
        <span>Mission record</span>
        <select
          value={mission?.id || ""}
          onChange={(e) => select(e.target.value)}
        >
          {missions.map((m) => (
            <option value={m.id} key={m.id}>
              {m.id} · {m.title}
            </option>
          ))}
        </select>
      </label>
      {error ? (
        <div className="notice danger">{error}</div>
      ) : !audit ? (
        <div className="loading">
          <Spinner /> Verifying the event chain…
        </div>
      ) : (
        <>
          <div
            className={`audit-verification ${audit.verified ? "" : "failed"}`}
          >
            <Fingerprint size={31} />
            <div>
              <h3>
                {audit.verified
                  ? "The local event chain verifies."
                  : "The event chain needs investigation."}
              </h3>
              <p>
                {audit.event_count} events · {audit.verification_scope}
              </p>
            </div>
            <Badge tone={audit.verified ? "sage" : "orange"}>
              {audit.verified ? "Verified" : "Failed"}
            </Badge>
          </div>
          <div className="audit-events">
            {audit.events.map((event, i) => (
              <article key={event.event_id}>
                <div className="audit-track">
                  <span>
                    {i === 0 ? <Radio size={15} /> : <Circle size={9} />}
                  </span>
                </div>
                <div>
                  <div className="audit-event-top">
                    <Badge>{pretty(event.kind)}</Badge>
                    <time>{date(event.recorded_at)} SAST</time>
                  </div>
                  <p>{event.detail}</p>
                  <span className="muted">Recorded by {event.actor}</span>
                  <details>
                    <summary>Event fingerprint</summary>
                    <code>{event.event_hash}</code>
                  </details>
                </div>
              </article>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
