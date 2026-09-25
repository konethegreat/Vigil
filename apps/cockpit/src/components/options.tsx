// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"use client";
import {
  Sparkles,
  FileText,
  ArrowUpRight,
  Search,
  ArrowRight,
  GitBranch,
  Check,
} from "lucide-react";
import { SectionHeading, Badge, Spinner, Empty } from "./primitives";
import type { Mission } from "@/lib/contracts";
import { pretty } from "@/lib/presentation";
export function Options({
  mission,
  generate,
  busy,
  write,
  source,
}: {
  mission: Mission;
  generate: () => void;
  busy: boolean;
  write: boolean;
  source: (id: string) => void;
}) {
  const plan = mission.plan;
  return (
    <>
      <SectionHeading
        label="REASONING YOU CAN INSPECT"
        title="A plan, with its workings."
        action={
          <button
            className="button primary small"
            disabled={busy || !write || !mission.evidence.length}
            onClick={generate}
          >
            {busy ? <Spinner /> : <Sparkles size={15} />}{" "}
            {busy
              ? "Analysing evidence…"
              : plan
                ? "Refresh plan"
                : "Prepare plan"}
          </button>
        }
      />
      {!plan ? (
        <Empty title="Begin with the evidence.">
          Generate a plan to connect the source records, expose gaps, and
          propose next steps. The result identifies whether a local model or a
          deterministic checklist was used.
        </Empty>
      ) : (
        <>
          <div className="plan-meta">
            <Badge tone={plan.mode === "LOCAL_MODEL" ? "purple" : "neutral"}>
              {plan.mode === "LOCAL_MODEL"
                ? "Local model analysis"
                : "Deterministic checklist"}
            </Badge>
            <span>{plan.model || "Rule-based baseline"}</span>
            <span>Evidence revision {plan.evidence_version}</span>
          </div>
          <p className="plan-summary">{plan.summary}</p>
          {plan.warnings.map((w) => (
            <div key={w} className="notice">
              {w}
            </div>
          ))}
          <div className="hypotheses">
            {plan.hypotheses.map((h, i) => (
              <article key={i}>
                <div className="hypothesis-label">
                  <span>HYPOTHESIS {String(i + 1).padStart(2, "0")}</span>
                  <Badge
                    tone={h.assessment === "DISPUTED" ? "orange" : "neutral"}
                  >
                    {pretty(h.assessment)}
                  </Badge>
                </div>
                <h4>{h.title}</h4>
                <p>{h.explanation}</p>
                <div className="citations">
                  {h.evidence_ids.map((id, j) => (
                    <button onClick={() => source(id)} key={id}>
                      <FileText size={12} />
                      {mission.evidence.find((e) => e.id === id)?.title ||
                        `Source ${j + 1}`}
                      <ArrowUpRight size={12} />
                    </button>
                  ))}
                </div>
              </article>
            ))}
          </div>
          <div className="plan-columns">
            <div>
              <h4>
                <Search size={16} />
                What is still unknown
              </h4>
              <ul>
                {plan.gaps.map((g) => (
                  <li key={g}>{g}</li>
                ))}
              </ul>
            </div>
            <div>
              <h4>
                <ArrowRight size={16} />
                Proposed next steps
              </h4>
              <ol>
                {plan.next_steps.map((s) => (
                  <li key={s}>{s}</li>
                ))}
              </ol>
            </div>
          </div>
          <div className="review-block">
            <GitBranch size={18} />
            <div>
              <strong>
                Independent model review · {pretty(plan.review.state)}
              </strong>
              {plan.review.concerns.length ? (
                <ul>
                  {plan.review.concerns.map((c) => (
                    <li key={c}>{c}</li>
                  ))}
                </ul>
              ) : (
                <p>
                  {plan.review.state === "COMPLETE"
                    ? "The reviewing model returned no concerns. Human assessment is still required."
                    : "No completed independent review is claimed for this plan."}
                </p>
              )}
            </div>
          </div>
          <details className="trace">
            <summary>
              Inspect execution trace <span>{plan.trace.length} stages</span>
            </summary>
            {plan.trace.map((stage, i) => (
              <div key={i}>
                <Check size={13} />
                <strong>{stage.role}</strong>
                <span>
                  {stage.status} · {stage.detail}
                </span>
              </div>
            ))}
            <p>
              Source IDs and structure are checked. Citation validity does not
              establish that a claim is supported; the owner must inspect the
              source.
            </p>
          </details>
        </>
      )}
    </>
  );
}
