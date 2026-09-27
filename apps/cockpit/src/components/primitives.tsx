// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mission workspace components
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"use client";
import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { Check, LoaderCircle, Waypoints, X } from "lucide-react";
export function Badge({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: string;
}) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
export function Spinner() {
  return <LoaderCircle size={16} className="spin" aria-hidden />;
}
export function Empty({
  title,
  children,
  icon = <Waypoints size={30} />,
}: {
  title: string;
  children: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <div className="empty">
      <span className="empty-icon">{icon}</span>
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
export function Dialog({
  title,
  children,
  close,
}: {
  title: string;
  children: ReactNode;
  close: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const el = ref.current;
    el?.showModal();
    return () => el?.close();
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={(e) => {
        e.preventDefault();
        close();
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) close();
      }}
      aria-labelledby="dialog-title"
    >
      <div className="dialog-heading">
        <div>
          <span className="eyebrow">VIGIL / MISSION RECORD</span>
          <h2 id="dialog-title">{title}</h2>
        </div>
        <button
          className="icon-button"
          aria-label="Close dialog"
          onClick={close}
        >
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function Form({
  children,
  onSubmit,
  busy,
  label = "Save record",
}: {
  children: ReactNode;
  onSubmit: (data: Record<string, string>) => Promise<void>;
  busy: boolean;
  label?: string;
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
    <form onSubmit={submit} className="record-form">
      {children}
      {error && (
        <div role="alert" className="notice danger">
          {error}
        </div>
      )}
      <div className="form-footer">
        <span>Changes are recorded in the mission history.</span>
        <button className="button primary" disabled={busy}>
          {busy ? <Spinner /> : <Check size={15} />} {busy ? "Saving…" : label}
        </button>
      </div>
    </form>
  );
}
export function Field({
  label,
  name,
  children,
  required = true,
  ...props
}: {
  label: string;
  name: string;
  children?: ReactNode;
  required?: boolean;
  multiline?: boolean;
  minLength?: number;
  maxLength?: number;
  defaultValue?: string;
  placeholder?: string;
}) {
  const { multiline, ...rest } = props;
  return (
    <label className="field">
      <span>{label}</span>
      {children ||
        (multiline ? (
          <textarea name={name} required={required} rows={4} {...rest} />
        ) : (
          <input name={name} required={required} {...rest} />
        ))}
    </label>
  );
}

export function SectionHeading({
  label,
  title,
  action,
}: {
  label: string;
  title: string;
  action?: ReactNode;
}) {
  return (
    <div className="section-heading">
      <div>
        <span className="eyebrow">{label}</span>
        <h3>{title}</h3>
      </div>
      {action}
    </div>
  );
}
