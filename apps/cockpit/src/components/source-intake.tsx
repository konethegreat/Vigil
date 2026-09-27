// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Preserved text intake
"use client";
import { useState, type ChangeEvent } from "react";
import { decodeSource, validateSourceText } from "@/lib/extractions";
import { pretty } from "@/lib/presentation";
import { Field, Form } from "./primitives";

export function SourceIntake({
  busy,
  save,
}: {
  busy: boolean;
  save: (data: Record<string, string>) => Promise<void>;
}) {
  const [original, setOriginal] = useState("");
  const [title, setTitle] = useState("");
  const [fileError, setFileError] = useState("");
  const [importing, setImporting] = useState(false);
  const [filename, setFilename] = useState("");
  async function importFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setFileError("");
    setImporting(true);
    try {
      if (file.size > 64000)
        throw new Error("The file exceeds 64 KB. No content was imported.");
      const text = decodeSource(file.name, await file.arrayBuffer());
      setOriginal(text);
      setFilename(file.name);
      setTitle((current) => current || file.name.slice(0, 160));
    } catch (error) {
      setFileError((error as Error).message);
    } finally {
      setImporting(false);
      event.target.value = "";
    }
  }
  return (
    <Form
      busy={busy || importing}
      label="Preserve source"
      onSubmit={async (data) => {
        if (importing) throw new Error("Wait for the file to finish loading.");
        validateSourceText(original);
        // Use the decoded buffer state: textarea serialization can normalise CRLF line endings.
        await save({ ...data, original });
      }}
    >
      <label className="field">
        <span>Import text file (optional)</span>
        <input
          type="file"
          accept=".txt,.md,text/plain,text/markdown"
          onChange={importFile}
          disabled={busy || importing}
        />
        <small>
          UTF-8 .txt or .md · up to 64 KB and 16,000 characters. The
          text-content hash excludes file metadata.
        </small>
      </label>
      {fileError && (
        <div role="alert" className="notice danger">
          {fileError}
        </div>
      )}
      <Field label="Source title" name="title">
        <input
          name="title"
          required
          minLength={3}
          maxLength={160}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </Field>
      <div className="field-pair">
        <Field
          label="Source or custodian"
          name="source"
          minLength={2}
          maxLength={160}
        />
        <Field label="Record type" name="kind">
          <select name="kind" defaultValue="DOCUMENT">
            {[
              "FIELD_REPORT",
              "DOCUMENT",
              "SENSOR",
              "SERVICE_UPDATE",
              "STATEMENT",
            ].map((kind) => (
              <option key={kind} value={kind}>
                {pretty(kind)}
              </option>
            ))}
          </select>
        </Field>
      </div>
      <Field
        label="What does this source report?"
        name="statement"
        multiline
        minLength={5}
        maxLength={2000}
      />
      <Field label="Original text to preserve" name="original">
        <textarea
          name="original"
          required
          minLength={5}
          maxLength={16000}
          rows={9}
          value={original}
          onChange={(e) => {
            setOriginal(e.target.value);
            setFilename("");
          }}
        />
      </Field>
      <p className="source-help">
        {filename ? `Imported ${filename}. ` : ""}
        {original.length.toLocaleString()} / 16,000 characters. New sources
        begin as unverified; existing plans and pending extractions need a fresh
        evidence snapshot.
      </p>
    </Form>
  );
}
