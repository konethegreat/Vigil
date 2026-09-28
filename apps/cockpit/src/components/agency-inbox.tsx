// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Joint cases, deliberate evidence release and accountable agency work
"use client";
import { useCallback, useEffect, useState } from "react";
import { ArrowDownToLine, Plus, RefreshCw } from "lucide-react";
import { api } from "@/lib/api";
import type { Identity } from "@/lib/contracts";
import {
  actionLabel,
  isoLocal,
  localInput,
  requestActions,
  type AgencyRequest,
  type CaseDetail,
  type CoordinationWorkspace,
  type RequestDetail,
} from "@/lib/coordination";
import { date, pretty } from "@/lib/presentation";
import { Dialog, Field } from "./primitives";
import {
  ExchangeForm,
  ObservationOriginal,
  ObservationPicker,
  useCoordination,
} from "./coordination-shared";

type Mutate = <T>(path: string, body: unknown) => Promise<T>;
export function AgencyInbox({
  identity,
  onBusy,
  openObservations,
}: {
  identity: Identity;
  onBusy: (busy: boolean) => void;
  openObservations: () => void;
}) {
  const { data, error, busy, loading, refresh, mutate } =
    useCoordination(onBusy);
  const [caseId, setCaseId] = useState(""),
    [create, setCreate] = useState(false),
    [filter, setFilter] = useState("All cases");
  const selected = data?.cases.find((c) => c.id === caseId) || data?.cases[0];
  const eligible = data?.agencies.some((a) => a.id === identity.agency_id);
  if (!data)
    return (
      <p role={error ? "alert" : "status"}>
        {error || "Loading agency cases…"}
      </p>
    );
  return (
    <div className="coordination-workspace">
      {error && (
        <p className="notice danger" role="alert">
          {error}
        </p>
      )}
      <div className="coordination-toolbar">
        <div className="segmented" role="group" aria-label="Agency case filter">
          {["All cases", "Incoming work", "Outgoing work"].map((f) => (
            <button
              key={f}
              aria-pressed={filter === f}
              disabled={busy}
              onClick={() => setFilter(f)}
            >
              {f}
            </button>
          ))}
        </div>
        <button
          className="button"
          disabled={busy || loading}
          onClick={() => {
            void refresh().catch(() => {});
          }}
        >
          <RefreshCw size={15} />
          Refresh inbox
        </button>
        <button
          className="button primary"
          disabled={busy || identity.read_only || !eligible}
          onClick={() => setCreate(true)}
        >
          <Plus size={15} />
          Open joint case
        </button>
        <button
          className="text-button"
          disabled={busy}
          onClick={openObservations}
        >
          Observations →
        </button>
      </div>
      {!eligible && (
        <p className="notice">
          Choose Gauteng coordination or Limpopo coordination to explore the
          two-agency demonstration. Other workspaces retain their own access
          boundaries.
        </p>
      )}
      {identity.read_only && (
        <p className="notice">
          Read-only agency review. Requests and evidence can be inspected;
          actions require a writing identity.
        </p>
      )}
      <div className="agency-layout">
        <aside className="case-register" aria-label="Joint case register">
          <h2>Joint cases</h2>
          {data.cases
            .filter(
              (c) =>
                filter === "All cases" ||
                data.requests.some(
                  (r) =>
                    r.case_id === c.id &&
                    (filter === "Incoming work"
                      ? r.recipient_tenant === identity.agency_id
                      : r.sender_tenant === identity.agency_id),
                ),
            )
            .map((c) => (
              <button
                key={c.id}
                className={c.id === selected?.id ? "selected" : ""}
                disabled={busy}
                onClick={() => setCaseId(c.id)}
              >
                <strong>{c.title}</strong>
                <span>{c.location}</span>
                <small>
                  {data.requests.filter((r) => r.case_id === c.id).length}{" "}
                  visible request
                  {data.requests.filter((r) => r.case_id === c.id).length === 1
                    ? ""
                    : "s"}
                </small>
              </button>
            ))}
          {!data.cases.length && (
            <p>No joint cases are visible in this workspace.</p>
          )}
        </aside>
        {selected ? (
          <CasePanel
            key={`${identity.actor}:${selected.id}`}
            caseId={selected.id}
            workspace={data}
            identity={identity}
            busy={busy}
            mutate={<T,>(path: string, body: unknown) => {
              setCaseId(selected.id);
              return mutate<T>(path, body);
            }}
          />
        ) : (
          <section className="case-panel">
            <h2>Connect evidence to responsible work</h2>
            <p>
              Open a joint case with one peer agency, select the observations to
              release, and record each request through to a result.
            </p>
          </section>
        )}
      </div>
      {create && (
        <Dialog
          title="Open a joint case"
          close={() => {
            if (!busy) setCreate(false);
          }}
        >
          <ExchangeForm
            busy={busy}
            label="Create shared case"
            onSubmit={async (form) => {
              const result = await mutate<{ id: string }>("cases", {
                ...form,
                occurred_at: isoLocal(form.occurred_at),
              });
              setCaseId(result.id);
              setCreate(false);
            }}
          >
            <p className="coordination-note">
              The named peer will see this case header and purpose. Source
              observations remain private until explicitly released.
            </p>
            <Field
              name="title"
              label="Case title"
              minLength={5}
              maxLength={160}
            />
            <Field
              name="objective"
              label="Objective"
              minLength={10}
              maxLength={1000}
              multiline
            />
            <Field
              name="location"
              label="Incident location"
              minLength={3}
              maxLength={160}
            />
            <label className="field">
              <span>Incident time</span>
              <input
                type="datetime-local"
                name="occurred_at"
                defaultValue={localInput()}
                required
              />
            </label>
            <Field name="peer_tenant" label="Participating agency">
              <select name="peer_tenant">
                {data.agencies
                  .filter((a) => a.id !== identity.agency_id)
                  .map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
              </select>
            </Field>
            <Field
              name="purpose"
              label="Shared case purpose"
              minLength={10}
              maxLength={500}
              multiline
            />
          </ExchangeForm>
        </Dialog>
      )}
    </div>
  );
}
function CasePanel({
  caseId,
  workspace,
  identity,
  busy,
  mutate,
}: {
  caseId: string;
  workspace: CoordinationWorkspace;
  identity: Identity;
  busy: boolean;
  mutate: Mutate;
}) {
  const [detail, setDetail] = useState<CaseDetail | null>(null),
    [error, setError] = useState(""),
    [requestId, setRequestId] = useState(""),
    [modal, setModal] = useState<
      "draft" | { request: RequestDetail; action: string } | null
    >(null),
    [selected, setSelected] = useState<string[]>([]),
    [now, setNow] = useState(Date.now);
  const reload = useCallback(async () => {
    setError("");
    try {
      setDetail(await api<CaseDetail>(`coordination/cases/${caseId}`));
    } catch (e) {
      setError((e as Error).message);
    }
  }, [caseId]);
  useEffect(() => {
    let cancelled = false;
    api<CaseDetail>(`coordination/cases/${caseId}`)
      .then((d) => {
        if (!cancelled) {
          setDetail(d);
          setError("");
        }
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [caseId, workspace.server_time]);
  useEffect(() => {
    if (!detail) return;
    const next = detail.requests
      .map((r) => Date.parse(r.expires_at))
      .filter((t) => t > now)
      .sort((a, b) => a - b)[0];
    if (!next) return;
    const timer = setTimeout(
      () => {
        setNow(Date.now());
        void reload();
      },
      Math.min(next - Date.now() + 30, 2147483647),
    );
    return () => clearTimeout(timer);
  }, [detail, now, reload]);
  const request =
    detail?.requests.find((r) => r.id === requestId) || detail?.requests[0];
  const agencyName = (id: string) =>
    workspace.agencies.find((a) => a.id === id)?.name || id;
  if (!detail)
    return (
      <section className="case-panel">
        <p role={error ? "alert" : "status"}>
          {error || "Loading shared case…"}
        </p>
      </section>
    );
  async function exportCase() {
    setError("");
    try {
      const exported = await api<CaseDetail>(
        `coordination/cases/${caseId}/export`,
      );
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(exported, null, 2)], {
          type: "application/json",
        }),
      );
      const a = document.createElement("a");
      a.href = url;
      a.download = `${caseId}-synthetic-case.json`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      setError((e as Error).message);
    }
  }
  function start(action: string, r: RequestDetail) {
    setSelected([]);
    setModal({ action, request: r });
  }
  return (
    <section className="case-panel" aria-label="Shared case">
      <header className="case-heading">
        <p className="mono-label">{detail.case.id}</p>
        <h2>{detail.case.title}</h2>
        <p>{detail.case.objective}</p>
        <div className="case-context">
          <span>{detail.case.location}</span>
          <span>{date(detail.case.occurred_at)}</span>
        </div>
        <p className="coordination-note">
          {agencyName(detail.case.owner_tenant)} ↔{" "}
          {agencyName(detail.case.peer_tenant)}
        </p>
        <details>
          <summary>Shared purpose</summary>
          <p>{detail.case.purpose}</p>
        </details>
        <div className="case-actions">
          <button
            className="button primary"
            disabled={
              busy || identity.read_only || !workspace.observations.length
            }
            onClick={() => {
              setSelected([]);
              setModal("draft");
            }}
          >
            <Plus size={15} />
            Prepare request
          </button>
          <button
            className="button"
            disabled={busy}
            onClick={() => void exportCase()}
          >
            <ArrowDownToLine size={15} />
            Export case
          </button>
          <button
            className="text-button"
            disabled={busy}
            onClick={() => void reload()}
          >
            Refresh case
          </button>
        </div>
      </header>
      {error && (
        <p className="notice danger" role="alert">
          {error}
        </p>
      )}
      <div className="request-register" aria-label="Case requests">
        {detail.requests.map((r) => (
          <button
            key={r.id}
            disabled={busy}
            className={r.id === request?.id ? "selected" : ""}
            onClick={() => setRequestId(r.id)}
          >
            <span>
              {r.recipient_tenant === identity.agency_id
                ? "INCOMING"
                : "OUTGOING"}
            </span>
            <strong>{r.title}</strong>
            <span>
              {pretty(r.state)}
              {r.revoked_at
                ? " · evidence revoked"
                : Date.parse(r.expires_at) <= now
                  ? " · evidence expired"
                  : ""}
            </span>
          </button>
        ))}
        {!detail.requests.length && (
          <p className="coordination-note">
            No released requests are visible. Prepare a request with selected
            evidence to start the handoff.
          </p>
        )}
      </div>
      {request && (
        <article
          className="request-detail"
          aria-label="Selected agency request"
        >
          <div className="coordination-section-heading">
            <h3>{request.title}</h3>
            <span className="request-state">{pretty(request.state)}</span>
          </div>
          <p>{request.task}</p>
          <dl className="observation-facts">
            <div>
              <dt>From</dt>
              <dd>{agencyName(request.sender_tenant)}</dd>
            </div>
            <div>
              <dt>To</dt>
              <dd>{agencyName(request.recipient_tenant)}</dd>
            </div>
            <div>
              <dt>Due</dt>
              <dd>{date(request.due_at)}</dd>
            </div>
            <div>
              <dt>Evidence access until</dt>
              <dd>{date(request.expires_at)}</dd>
            </div>
            <div>
              <dt>Responsibility accepted by</dt>
              <dd>{request.accepted_by || "Awaiting acceptance"}</dd>
            </div>
            <div>
              <dt>Request reference</dt>
              <dd className="long-hash">{request.id}</dd>
            </div>
          </dl>
          <details>
            <summary>Reason for sharing</summary>
            <p>{request.release_reason}</p>
          </details>
          {(request.revoked_at || Date.parse(request.expires_at) <= now) && (
            <p className="notice">
              Evidence access {request.revoked_at ? "revoked" : "expired"}.
              Previously communicated task notes remain in the case. Packet
              owners retain their originals.
            </p>
          )}
          <div className="case-actions">
            {requestActions(request, identity, now).map((action) => (
              <button
                key={action}
                className={
                  action === "REVOKED" || action === "CANCELLED"
                    ? "text-button"
                    : "button"
                }
                disabled={busy}
                onClick={() => start(action, request)}
              >
                {actionLabel[action]}
              </button>
            ))}
          </div>
          <div className="packet-list">
            <h3>
              {request.state === "DRAFT"
                ? "Draft evidence"
                : "Released evidence"}
            </h3>
            {request.state === "DRAFT" && (
              <p className="coordination-note">
                Draft packet · visible only to the sending agency. Inspect each
                original before releasing.
              </p>
            )}
            {request.packets
              .filter(
                (p) =>
                  p.content.owner_tenant === identity.agency_id ||
                  (!p.revoked_at && Date.parse(p.content.expires_at) > now),
              )
              .map((packet) => (
                <section key={packet.content.kind} className="evidence-packet">
                  <h4>
                    {packet.content.kind === "RESULT"
                      ? "Result evidence"
                      : "Request evidence"}
                  </h4>
                  <p>
                    {agencyName(packet.content.owner_tenant)} ·{" "}
                    {packet.content.observations.length} selected observation
                    {packet.content.observations.length === 1 ? "" : "s"}
                  </p>
                  {packet.content.observations.map((o) => (
                    <details key={o.id}>
                      <summary>{o.description}</summary>
                      <ObservationOriginal observation={o} />
                    </details>
                  ))}
                  <details>
                    <summary>Packet fingerprint and release</summary>
                    <code className="long-hash">{packet.fingerprint}</code>
                    <p>
                      Prepared by {packet.content.released_by} ·{" "}
                      {date(packet.content.prepared_at)}. Expires{" "}
                      {date(packet.content.expires_at)}.
                    </p>
                  </details>
                </section>
              ))}
            {!request.packets.some(
              (p) =>
                p.content.owner_tenant === identity.agency_id ||
                (!p.revoked_at && Date.parse(p.content.expires_at) > now),
            ) && (
              <p className="coordination-note">
                No packet contents are available under your current release.
              </p>
            )}
          </div>
          <div className="request-history">
            <div className="coordination-section-heading">
              <h3>Request history</h3>
              <span>
                {request.audit.verified
                  ? "Receipt chain verified"
                  : "Receipt verification failed"}
              </span>
            </div>
            <ol>
              {request.receipts.map((receipt) => (
                <li key={receipt.id}>
                  <div>
                    <strong>{pretty(receipt.kind)}</strong>
                    <time>{date(receipt.at)}</time>
                  </div>
                  <p>{receipt.note}</p>
                  <small>
                    {receipt.actor} · {agencyName(receipt.agency)}
                  </small>
                </li>
              ))}
            </ol>
            <p className="coordination-note">
              Local durable inbox. Acknowledgments are recorded by the receiving
              team. No external agency or dispatch service is contacted.
            </p>
          </div>
        </article>
      )}
      {modal && (
        <Dialog
          title={
            modal === "draft"
              ? "Prepare an agency request"
              : actionLabel[modal.action]
          }
          close={() => {
            if (!busy) setModal(null);
          }}
        >
          <ExchangeForm
            busy={busy}
            label={
              modal === "draft"
                ? "Save private draft"
                : actionLabel[modal.action]
            }
            onSubmit={async (form) => {
              if (modal === "draft") {
                const result = await mutate<AgencyRequest>("requests", {
                  ...form,
                  case_id: caseId,
                  observation_ids: selected,
                  due_at: isoLocal(form.due_at),
                  expires_at: isoLocal(form.expires_at),
                });
                setRequestId(result.id);
              } else
                await mutate(`requests/${modal.request.id}/commands`, {
                  ...form,
                  action: modal.action,
                  expected_version: modal.request.version,
                  ...(modal.action === "RESULT_RECORDED"
                    ? { observation_ids: selected }
                    : {}),
                });
              setModal(null);
              setNow(Date.now());
              await reload();
            }}
          >
            {modal === "draft" ? (
              <>
                <Field
                  name="title"
                  label="Request title"
                  minLength={5}
                  maxLength={160}
                />
                <Field
                  name="task"
                  label="Specific task for the receiving agency"
                  minLength={10}
                  maxLength={1000}
                  multiline
                />
                <div className="form-row">
                  <label className="field">
                    <span>Response due</span>
                    <input
                      type="datetime-local"
                      name="due_at"
                      defaultValue={localInput(new Date(now + 3600000 * 4))}
                      required
                    />
                  </label>
                  <label className="field">
                    <span>Evidence access expires</span>
                    <input
                      type="datetime-local"
                      name="expires_at"
                      defaultValue={localInput(new Date(now + 86400000))}
                      required
                    />
                  </label>
                </div>
                <Field
                  name="release_reason"
                  label="Why this evidence should be shared"
                  minLength={10}
                  maxLength={1000}
                  multiline
                />
                <ObservationPicker
                  observations={workspace.observations}
                  selected={selected}
                  setSelected={setSelected}
                />
              </>
            ) : (
              <>
                <p className="coordination-note">
                  {modal.action === "RELEASED"
                    ? "This releases the selected packet into the receiving agency's local inbox. Receipt and acceptance are recorded separately."
                    : modal.action === "RESULT_RECORDED"
                      ? "Explain the result and release selected observations back to the requester. An inconclusive finding is a valid result when its limits are stated."
                      : modal.action === "REVOKED"
                        ? "Future retrieval of your request packet will stop. Downloaded copies and already communicated task notes cannot be recalled."
                        : modal.action === "CLOSED"
                          ? "Confirm that the recorded result or explicit unknown outcome has been reviewed. Closure does not convert uncertain evidence into verified fact."
                          : "Record what happened and who is taking responsibility. This creates an immutable receipt."}
                </p>
                <Field
                  name="note"
                  label={
                    modal.action === "RESULT_RECORDED"
                      ? "Result and limitations"
                      : "Receipt explanation"
                  }
                  minLength={10}
                  maxLength={1500}
                  multiline
                />
                {modal.action === "RESULT_RECORDED" && (
                  <ObservationPicker
                    observations={workspace.observations}
                    selected={selected}
                    setSelected={setSelected}
                  />
                )}
              </>
            )}
          </ExchangeForm>
        </Dialog>
      )}
    </section>
  );
}
