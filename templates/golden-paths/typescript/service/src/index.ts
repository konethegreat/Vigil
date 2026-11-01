// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Public surface of the service template (P1 task 1.4)
export { buildApp, MissingPolicyHeaders } from "./app.js";
export type { AppOptions } from "./app.js";

export { ConfigurationError, loadSettings } from "./config.js";
export type { Environment, LogLevel, Settings } from "./config.js";

export {
  PolicyDenied,
  PolicyUnavailable,
  UnavailableWarden,
} from "./policy.js";
export type {
  AuthorizeRequest,
  PolicyContext,
  WardenBinding,
} from "./policy.js";

export { otlpExporter, startTelemetry } from "./telemetry.js";
