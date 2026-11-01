// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Injected library configuration (P1 task 1.4)
/**
 * Library settings are supplied by the caller.
 *
 * A library never reads the environment: the service or job that imports it
 * owns configuration and validation (BACKEND_GUIDE §2.4, §9.3).
 */

export interface LibrarySettings {
  readonly namespace: string;
  readonly batchSize: number;
}

export interface LibrarySettingsInput {
  readonly namespace: string;
  readonly batchSize?: number;
}

const DEFAULT_BATCH_SIZE = 100;

export function librarySettings(input: LibrarySettingsInput): LibrarySettings {
  const namespace = input.namespace.trim();
  if (namespace.length === 0) {
    throw new RangeError("namespace must not be empty");
  }

  const batchSize = input.batchSize ?? DEFAULT_BATCH_SIZE;
  if (!Number.isInteger(batchSize) || batchSize < 1) {
    throw new RangeError("batchSize must be a positive integer");
  }

  return Object.freeze({ namespace, batchSize });
}
