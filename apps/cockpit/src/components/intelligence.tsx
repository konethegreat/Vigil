// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"use client";
import {
  Waypoints,
  FileText,
  ArrowRight,
  ShieldCheck,
  CheckCheck,
} from "lucide-react";
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
      <div className="intelligence-hero">
        <div className="intelligence-emblem">
          <Waypoints size={56} strokeWidth={1} />
        </div>
        <div>
          <span className="eyebrow">THE INTELLIGENCE FABRIC</span>
          <h2>
            Many perspectives.
            <br />
            One accountable record.
          </h2>
          <p>
            Evidence enters a bounded analysis pipeline. A reasoning model
            proposes an interpretation. A second model can challenge it. The
            mission owner decides what becomes action.
          </p>
        </div>
      </div>
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
          <div className="model-grid">
            {[
              {
                n: "01",
                title: "Reasoner",
                model: i.reasoner_model,
                ready: i.mode === "LOCAL_MODEL",
                text: "Connects supplied source records, distinguishes hypotheses, and proposes bounded next steps.",
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
                text: "Checks schema, source IDs and evidence revision. It does not decide whether a claim is true.",
              },
            ].map((m) => (
              <article key={m.n}>
                <span className="eyebrow">SPECIALIST / {m.n}</span>
                <h3>{m.title}</h3>
                <p>{m.text}</p>
                <div>
                  <Badge tone={m.ready ? "sage" : "neutral"}>
                    {m.ready ? "Configured" : "Not configured"}
                  </Badge>
                  <code>{m.model || "No model selected"}</code>
                </div>
              </article>
            ))}
          </div>
          <div className="architecture-line">
            <span>
              <FileText size={17} />
              Evidence snapshot
            </span>
            <ArrowRight size={16} />
            <span>
              <Waypoints size={17} />
              Bounded analysis
            </span>
            <ArrowRight size={16} />
            <span>
              <ShieldCheck size={17} />
              Human assessment
            </span>
            <ArrowRight size={16} />
            <span>
              <CheckCheck size={17} />
              Recorded outcome
            </span>
          </div>
          <div className="notice">
            Configured models can still fail at runtime. Every plan records the
            path actually used. Model output cannot execute a task or alter a
            source record.
          </div>
        </>
      )}
    </section>
  );
}
