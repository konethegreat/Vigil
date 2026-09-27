// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// Modified for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Plain model operations table
"use client";
import { Badge, Spinner } from "./primitives";
import type { Intelligence } from "@/lib/contracts";
import { pretty } from "@/lib/presentation";
export function IntelligenceView({
  intelligence: i,
  error,
}: {
  intelligence: Intelligence | null;
  error: string;
}) {
  return (
    <section className="intelligence-view">
      {error ? (
        <div className="notice danger">{error}</div>
      ) : !i ? (
        <div className="loading">
          <Spinner /> Checking intelligence service…
        </div>
      ) : (
        <>
          <div className="intelligence-status">
            <span className="connection-dot" />
            <strong>
              {i.mode === "LOCAL_MODEL"
                ? "Local reasoning configured"
                : "Baseline planning available"}
            </strong>
            <Badge tone={i.mode === "LOCAL_MODEL" ? "sage" : "neutral"}>
              {pretty(i.state || "unavailable")}
            </Badge>
            <p>{i.message}</p>
          </div>
          <div className="model-table-wrap">
            <table className="model-table">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Model / engine</th>
                  <th>Availability</th>
                  <th>Work performed</th>
                </tr>
              </thead>
              <tbody>
                {[
                  {
                    n: "01",
                    title: "Reasoner",
                    model: i.reasoner_model,
                    ready: i.mode === "LOCAL_MODEL",
                    text: "Extracts quoted source proposals and prepares evidence-based mission plans.",
                  },
                  {
                    n: "02",
                    title: "Challenger",
                    model: i.challenger_model,
                    ready: i.challenger_ready,
                    text: "Reviews the proposed analysis for unsupported claims, contradictions, and missing evidence.",
                  },
                  {
                    n: "03",
                    title: "Contract verifier",
                    model: "Deterministic checks",
                    ready: true,
                    text: "Checks structure, quotations, source IDs and evidence revisions. Does not establish truth.",
                  },
                ].map((m) => (
                  <tr key={m.n}>
                    <th scope="row">{m.title}</th>
                    <td>
                      <code>{m.model || "No model selected"}</code>
                    </td>
                    <td>
                      {m.n === "03"
                        ? "Built in"
                        : m.ready
                          ? "Installed locally"
                          : m.model
                            ? "Unavailable"
                            : "Not configured"}
                    </td>
                    <td>{m.text}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="notice">
            Configured models can still fail at runtime. Every plan records the
            path actually used. Extraction requires a working model and a final
            analyst decision before any proposal becomes an investigation
            record.
          </div>
        </>
      )}
    </section>
  );
}
