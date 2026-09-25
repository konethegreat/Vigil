// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"use client";
import {
  FileText,
  Target,
  Waypoints,
  CheckCheck,
  GitBranch,
  ArrowRight,
  ArrowUpRight,
} from "lucide-react";
import { SectionHeading, Spinner } from "./primitives";
import type { Mission, MissionTab as Tab } from "@/lib/contracts";
import { pretty } from "@/lib/presentation";
export function Situation({
  mission,
  current,
  setTab,
  generate,
  busy,
  write,
}: {
  mission: Mission;
  current: boolean;
  setTab: (t: Tab) => void;
  generate: () => void;
  busy: boolean;
  write: boolean;
}) {
  const supported = mission.evidence.filter(
      (e) => e.status === "SUPPORTED",
    ).length,
    disputed = mission.evidence.filter((e) => e.status === "DISPUTED").length,
    confirmed = mission.tasks.filter((t) => t.state === "CONFIRMED").length;
  return (
    <>
      <div className="objective">
        <Target size={20} />
        <div>
          <span className="eyebrow">THE OBJECTIVE</span>
          <p>{mission.objective}</p>
        </div>
      </div>
      <div className="situation-split">
        <div>
          <SectionHeading
            label="THE OPERATIONAL PICTURE"
            title="See how the work connects."
          />
          <div className="relation-canvas">
            <span className="canvas-label">
              MISSION RELATIONSHIPS <span>·</span> NOT A GEOGRAPHIC MAP
            </span>
            <div className="relation-flow">
              <button
                className="flow-node source"
                onClick={() => setTab("Evidence")}
              >
                <FileText size={21} />
                <strong>{mission.evidence.length} sources</strong>
                <span>
                  {disputed ? `${disputed} disputed` : `${supported} supported`}
                </span>
              </button>
              <span className="connector" />
              <button
                className="flow-node center"
                onClick={() => setTab("Options")}
              >
                <Waypoints size={23} />
                <strong>
                  {mission.domain === "PROTECT"
                    ? "Continuity of care"
                    : "Mission picture"}
                </strong>
                <span>
                  {current
                    ? "Plan is current"
                    : mission.plan
                      ? "Plan needs refresh"
                      : "Plan to be prepared"}
                </span>
              </button>
              <span className="connector" />
              <button
                className="flow-node result"
                onClick={() => setTab("Operations")}
              >
                <CheckCheck size={21} />
                <strong>
                  {confirmed} / {mission.tasks.length} results
                </strong>
                <span>Verified task outcomes</span>
              </button>
            </div>
            <div className="canvas-legend">
              <span>
                <i />
                Evidence
              </span>
              <span>
                <i />
                Decision
              </span>
              <span>
                <i />
                Accountability
              </span>
            </div>
          </div>
        </div>
        <aside className="next-move">
          <span className="eyebrow">THE NEXT MOVE</span>
          <span className="next-icon">
            {mission.outcome ? (
              <CheckCheck size={21} />
            ) : (
              <GitBranch size={21} />
            )}
          </span>
          <h4>
            {mission.outcome
              ? "Keep the outcome accessible."
              : !mission.evidence.length
                ? "Establish the first source."
                : !current
                  ? "Turn the sources into a plan."
                  : confirmed < mission.tasks.length
                    ? "Make the next handoff count."
                    : "Record what changed."}
          </h4>
          <p>
            {mission.outcome
              ? "The original evidence and decision trail stay attached to the completed mission."
              : !current
                ? "Surface contradictions, identify what is missing, and give the owner a grounded next step."
                : "Keep acceptance, delivery, and verification visible to the mission owner."}
          </p>
          <button
            className="text-button"
            disabled={busy || (!current && !write && !!mission.evidence.length)}
            onClick={() => {
              if (mission.outcome) setTab("Outcome");
              else if (!mission.evidence.length) setTab("Evidence");
              else if (!current) generate();
              else
                setTab(
                  confirmed < mission.tasks.length ? "Operations" : "Outcome",
                );
            }}
          >
            {busy ? (
              <Spinner />
            ) : mission.outcome ? (
              "View outcome"
            ) : !mission.evidence.length ? (
              "Add evidence"
            ) : !current ? (
              "Prepare plan"
            ) : confirmed < mission.tasks.length ? (
              "Open operations"
            ) : (
              "Record outcome"
            )}
            <ArrowRight size={16} />
          </button>
        </aside>
      </div>
      <div className="signal-summary">
        <div>
          <span className="summary-value">
            {supported.toString().padStart(2, "0")}
          </span>
          <span>
            Supported sources<small>Assessed by the mission owner</small>
          </span>
        </div>
        <div>
          <span className="summary-value warning-text">
            {disputed.toString().padStart(2, "0")}
          </span>
          <span>
            Conflicting sources<small>Kept visible until reconciled</small>
          </span>
        </div>
        <div>
          <span className="summary-value">
            {mission.tasks
              .filter((t) => t.state === "SENT")
              .length.toString()
              .padStart(2, "0")}
          </span>
          <span>
            Awaiting acceptance<small>A send receipt is not a handoff</small>
          </span>
        </div>
      </div>
      <div className="latest-evidence">
        <div className="inline-heading">
          <span className="eyebrow">LATEST SOURCE ON RECORD</span>
          <button className="text-button" onClick={() => setTab("Evidence")}>
            View register
            <ArrowUpRight size={14} />
          </button>
        </div>
        {mission.evidence.length ? (
          <p>
            <FileText size={15} />
            <strong>{mission.evidence.at(-1)!.title}</strong>
            <span>{pretty(mission.evidence.at(-1)!.status)}</span>
          </p>
        ) : (
          <p>No source has been added yet.</p>
        )}
      </div>
    </>
  );
}
