// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Source proposals and text intake
import type { Mission } from "./contracts";
import type { ObjectType, relations } from "./investigations";

export type ObjectProposal = {
  id: string;
  object_type: ObjectType;
  title: string;
  summary: string;
  quote: string;
};
export type LinkProposal = {
  id: string;
  from_id: string;
  to_id: string;
  relation: (typeof relations)[number];
  reason: string;
  quote: string;
};
export type Extraction = {
  id: string;
  mission_id: string;
  evidence_id: string;
  evidence_version: number;
  original_sha256: string;
  created_at: string;
  created_by: string;
  model: string;
  proposal: {
    objects: ObjectProposal[];
    links: LinkProposal[];
    gaps: string[];
  };
  review: {
    state: "NOT_RUN" | "COMPLETE" | "UNAVAILABLE";
    model: string | null;
    concerns: string[];
  };
  warnings: string[];
  stale: boolean;
  decision: null | {
    object_ids: string[];
    link_ids: string[];
    object_records: Record<string, string>;
    link_records: Record<string, string>;
    actor: string;
    reviewed_at: string;
    note: string;
  };
};
export type ExtractionResult = { run: Extraction; mission: Mission };

export function decodeSource(name: string, bytes: ArrayBuffer): string {
  if (!/\.(txt|md)$/i.test(name))
    throw new Error("Choose a UTF-8 .txt or .md file.");
  if (bytes.byteLength > 64000)
    throw new Error("The file exceeds 64 KB. No content was imported.");
  let text: string;
  try {
    // Preserve decoded BOM and line endings. The saved hash covers this text, not file metadata.
    text = new TextDecoder("utf-8", { fatal: true, ignoreBOM: true }).decode(
      bytes,
    );
  } catch {
    throw new Error(
      "This file is not valid UTF-8 text. No content was imported.",
    );
  }
  validateSourceText(text);
  return text;
}

export function validateSourceText(text: string) {
  if (
    text.length < 5 ||
    text.length > 16000 ||
    !text.trim() ||
    text.includes("\0")
  )
    throw new Error(
      "Use 5–16,000 characters of text without NUL bytes. Content is never truncated.",
    );
}

export function retainLinks(
  links: LinkProposal[],
  objects: string[],
  selected: string[],
) {
  return selected.filter((id) =>
    links.some(
      (link) =>
        link.id === id &&
        objects.includes(link.from_id) &&
        objects.includes(link.to_id),
    ),
  );
}
